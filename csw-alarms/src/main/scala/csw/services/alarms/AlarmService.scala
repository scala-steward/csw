package csw.services.alarms

import csw.services.alarms.models.AlarmEntity.CurrentSeverity
import csw.services.alarms.models.{AlarmEntity, AlarmKey}

import scala.concurrent.Future

trait AlarmService extends AlarmClientService {

  def getAlarm(key: AlarmKey): Future[AlarmEntity]

  def getSeverity(alarmKey: AlarmKey): Future[CurrentSeverity]
}

object AlarmService {

  val defaultRefreshSecs = 5

  val maxMissedRefresh = 3

}
