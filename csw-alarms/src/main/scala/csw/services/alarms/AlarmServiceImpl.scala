package csw.services.alarms
import java.util.concurrent.TimeUnit

import akka.actor.ActorRefFactory
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.services.alarms.AlarmService.defaultRefreshSecs
import csw.services.alarms.internal.RefreshAlarmMessages.SetSeverity
import csw.services.alarms.internal.{RefreshAlarmBehaviorFactory, RefreshAlarmMessages}
import csw.services.alarms.models.AlarmEntity.{CurrentSeverity, SeverityLevel}
import csw.services.alarms.models.AlarmState.{AcknowledgedState, LatchedState}
import csw.services.alarms.models.{AlarmEntity, AlarmKey, AlarmState, MinimalAlarmEntity}
import scredis.Redis

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object AlarmServiceImpl {

  val refreshSecs: Int =
    Option(System.getProperty("csw.services.alarms.refreshSecs"))
      .getOrElse(s"$defaultRefreshSecs")
      .toInt

}

class AlarmServiceImpl(
    redis: Redis,
    autoRefresh: Boolean,
    context: ActorContext[_]
)(implicit system: ActorRefFactory, timeout: Timeout)
    extends AlarmService {

  lazy val alarmRefreshActor: ActorRef[RefreshAlarmMessages] =
    context.spawn(RefreshAlarmBehaviorFactory.make(this, Map.empty), "RefreshAlarm")

  override def getAlarm(key: AlarmKey): Future[AlarmEntity] = getAlarm(key.key).map {
    case None    ⇒ throw new RuntimeException(s"No alarm was found for key $key")
    case Some(x) => x
  }

  override def getSeverity(alarmKey: AlarmKey): Future[CurrentSeverity] = {
    for {
      alarm            <- getMinimalAlarmEntity(alarmKey)
      alarmState       <- getAlarmState(alarmKey)
      reportedSeverity <- getReportedSeverity(alarmKey)
    } yield {
      getCurrentSeverity(alarmKey, alarm, alarmState, reportedSeverity)
    }
  }

  override def setSeverity(alarmKey: AlarmKey, severity: AlarmEntity.SeverityLevel): Future[Unit] = {
    val f1 = getMinimalAlarmEntity(alarmKey)
    val f2 = getAlarmState(alarmKey)
    val futureResult = for {
      alarm      <- f1
      alarmState <- f2
      result     <- setSeverity(alarmKey, alarm, alarmState, severity)
    } yield result

    if (autoRefresh)
      alarmRefreshActor ! SetSeverity(Map(alarmKey -> severity), setNow = false)

    futureResult
  }

  private[alarms] def getAlarm(key: String): Future[Option[AlarmEntity]] = redis.hGetAll(key).map { x ⇒
    x.flatMap(AlarmEntity(_))
  }

  private def getCurrentSeverity(
      alarmKey: AlarmKey,
      alarm: MinimalAlarmEntity,
      alarmState: AlarmState,
      reportedSeverity: SeverityLevel
  ): CurrentSeverity = {

    val latchedSeverity =
      if (alarmState.latchedState == LatchedState.Normal) reportedSeverity
      else alarmState.latchedSeverity
    CurrentSeverity(reportedSeverity, latchedSeverity)

  }

  private def getMinimalAlarmEntity(key: AlarmKey): Future[MinimalAlarmEntity] =
    redis
      .hmGet(key.key, "severityLevels", "acknowledge", "latched")
      .map(MinimalAlarmEntity(_))
      .map { opt =>
        if (opt.isEmpty) {
          throw new RuntimeException(s"No alarm was found for key $key")
        } else opt.get
      }

  private def getAlarmState(key: AlarmKey): Future[AlarmState] = redis.hGetAll(key.stateKey).map {
    case None    ⇒ throw new RuntimeException(s"Alarm state for $key not found.")
    case Some(x) ⇒ AlarmState(x)
  }

  private def getReportedSeverity(alarmKey: AlarmKey): Future[SeverityLevel] =
    for {
      exists <- redis.exists(alarmKey.severityKey)
      sevStrOpt <- if (exists) redis.get[String](alarmKey.severityKey)
      else Future.successful(None)
    } yield {
      sevStrOpt.flatMap(SeverityLevel(_)).getOrElse(SeverityLevel.Disconnected)
    }

  private def setSeverity(
      alarmKey: AlarmKey,
      alarm: MinimalAlarmEntity,
      alarmState: AlarmState,
      severity: SeverityLevel
  ): Future[Unit] = {

    if (isSeveritySupported(alarm, severity)) {

      val latchedSeverity = getLatchedSeverity(alarm, alarmState, severity)
      val s               = if (latchedSeverity != severity) s" (latched: $latchedSeverity)" else ""
      println(s"Setting severity for $alarmKey to $severity$s")

      redis
        .inTransaction { transaction ⇒
          transaction.set(alarmKey.severityKey,
                          severity.name,
                          ttlOpt =
                            Some(FiniteDuration(AlarmServiceImpl.refreshSecs * AlarmService.maxMissedRefresh, TimeUnit.SECONDS)))

          if (alarm.latched && severity.isAlarm && alarmState.latchedState == LatchedState.Normal) {
            println(s"Setting latched state for $alarmKey to NeedsReset")
            transaction.hSet(alarmKey.stateKey, AlarmState.latchedStateField, LatchedState.NeedsReset.name)
          } else Future.successful(true)

          if (alarm.latched && severity.isAlarm && latchedSeverity != alarmState.latchedSeverity) {
            println(s"Setting latched severity for $alarmKey to $latchedSeverity")
            transaction.hSet(alarmKey.stateKey, AlarmState.latchedSeverityField, latchedSeverity.name)
          } else Future.successful(true)

          if (alarm.acknowledge && severity.isAlarm && alarmState.acknowledgedState == AcknowledgedState.Normal) {
            println(s"Setting acknowledged state for $alarmKey to NeedsAcknowledge")
            transaction.hSet(alarmKey.stateKey, AlarmState.acknowledgedStateField, AcknowledgedState.NeedsAcknowledge.name)
          } else Future.successful(true)

        }
    } else
      println(s"Alarm $alarmKey is not listed as supporting severity level $severity")

    Future.unit
  }

  private def getLatchedSeverity(alarm: MinimalAlarmEntity, alarmState: AlarmState, severity: SeverityLevel): SeverityLevel = {

    def isLatchedStateNormal              = alarmState.latchedState == LatchedState.Normal
    def isSeverityMoreThanLatchedSeverity = severity.level > alarmState.latchedSeverity.level

    if (alarm.latched && !isLatchedStateNormal && !isSeverityMoreThanLatchedSeverity) {
      alarmState.latchedSeverity
    } else severity
  }

  private def isSeveritySupported(alarm: MinimalAlarmEntity, severity: SeverityLevel) =
    severity != SeverityLevel.Disconnected && !alarm.severityLevels
      .contains(severity)

}
