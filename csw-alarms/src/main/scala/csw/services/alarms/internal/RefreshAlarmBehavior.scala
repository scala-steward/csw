package csw.services.alarms.internal

import akka.typed.Behavior
import akka.typed.scaladsl.{Actor, TimerScheduler}
import csw.services.alarms.internal.RefreshAlarmMessages.{Publish, SetSeverity}
import csw.services.alarms.models.AlarmEntity.SeverityLevel
import csw.services.alarms.models.AlarmKey
import csw.services.alarms.{AlarmService, AlarmServiceImpl}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class RefreshAlarmBehavior(
    alarmService: AlarmService,
    initialMap: Map[AlarmKey, SeverityLevel],
    timerScheduler: TimerScheduler[RefreshAlarmMessages],
) extends Actor.MutableBehavior[RefreshAlarmMessages] {

  val delay: FiniteDuration                         = AlarmServiceImpl.refreshSecs.seconds
  var alarmToSeverity: Map[AlarmKey, SeverityLevel] = initialMap

  timerScheduler.startPeriodicTimer("Refresh", Publish, delay)

  override def onMessage(msg: RefreshAlarmMessages): Behavior[RefreshAlarmMessages] = msg match {

    case SetSeverity(alarms, setNow) ⇒
      alarmToSeverity = alarmToSeverity ++ alarms
      if (setNow)
        alarmToSeverity.map { case (alarmKey, severity) ⇒ alarmService.setSeverity(alarmKey, severity) }
      this
    case Publish ⇒
      alarmToSeverity.map { case (alarmKey, severity) ⇒ alarmService.setSeverity(alarmKey, severity) }
      this

  }
}
