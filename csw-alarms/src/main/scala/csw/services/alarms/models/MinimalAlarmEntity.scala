package csw.services.alarms.models

import csw.services.alarms.models.AlarmEntity.SeverityLevel

private[alarms] case class MinimalAlarmEntity(severityLevels: List[SeverityLevel], acknowledge: Boolean, latched: Boolean)

private[alarms] object MinimalAlarmEntity {
  def apply(seq: Seq[Option[String]]): Option[MinimalAlarmEntity] = {
    for {
      severityLevels <- seq.head.map(_.split(":").toList.map(SeverityLevel(_).getOrElse(SeverityLevel.Disconnected)))
      acknowledge    <- seq(1).map(_.toBoolean)
      latched        <- seq(2).map(_.toBoolean)
    } yield MinimalAlarmEntity(severityLevels, acknowledge, latched)
  }
}
