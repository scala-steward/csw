package csw.services.alarms.internal

import csw.services.alarms.models.AlarmEntity.SeverityLevel
import csw.services.alarms.models.AlarmKey

sealed trait RefreshAlarmMessages
object RefreshAlarmMessages {
  case class SetSeverity(alarms: Map[AlarmKey, SeverityLevel], setNow: Boolean) extends RefreshAlarmMessages
  case object Publish                                                           extends RefreshAlarmMessages
}
