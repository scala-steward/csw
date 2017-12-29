package csw.services.alarms

import csw.services.alarms.models.AlarmEntity.SeverityLevel
import csw.services.alarms.models.AlarmKey

import scala.concurrent.Future

trait AlarmClientService {

  def setSeverity(alarmKey: AlarmKey, severity: SeverityLevel): Future[Unit]

}
