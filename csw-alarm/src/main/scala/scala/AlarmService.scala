package scala

import models.{AlarmKey, Severity}

trait AlarmService {
  def setSeverity(alarmKey: AlarmKey, severity: Severity)
}
