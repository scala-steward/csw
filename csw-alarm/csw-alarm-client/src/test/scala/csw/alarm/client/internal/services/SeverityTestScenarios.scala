package csw.alarm.client.internal.services
import csw.alarm.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.alarm.models.AlarmSeverity.{Critical, Major, Okay, Warning}
import csw.alarm.models.FullAlarmSeverity.Disconnected
import csw.alarm.models.Key.AlarmKey
import csw.alarm.client.internal.helpers.{SetSeverityAckStatusTestCase, SetSeverityTestCase}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.AOESW

object SeverityTestScenarios {

  val SeverityTestCases = List(
    SetSeverityTestCase(
      alarmKey = AlarmKey(
        Prefix(AOESW, "test_component"),
        "setSeverity, should increase latched severity, when new severity is higher than old and alarm is still initializing"
      ),
      oldLatchedSeverity = Warning,
      newSeverity = Critical,
      expectedLatchedSeverity = Critical,
      initializing = true,
      expectedInitializing = false
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(
        Prefix(AOESW, "test_component"),
        "setSeverity, should increase latched severity, when new severity is higher than old and alarm is done initializing"
      ),
      oldLatchedSeverity = Disconnected,
      newSeverity = Critical,
      expectedLatchedSeverity = Critical,
      initializing = false,
      expectedInitializing = false
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(
        Prefix(AOESW, "test_component"),
        "setSeverity, should decrease latched severity, when new severity is lower than old but alarm is still initializing"
      ),
      oldLatchedSeverity = Disconnected,
      newSeverity = Okay,
      expectedLatchedSeverity = Okay,
      initializing = true,
      expectedInitializing = false
    ),
    SetSeverityTestCase(
      alarmKey = AlarmKey(
        Prefix(AOESW, "test_component"),
        "setSeverity, should not update latched severity, when new severity is lower than old and alarm is done initializing"
      ),
      oldLatchedSeverity = Major,
      newSeverity = Warning,
      expectedLatchedSeverity = Major,
      initializing = false,
      expectedInitializing = false
    )
  )

  val AckStatusTestCases = List(
    // ====== Severity change but not to Okay =====
    // AckStatus = Unacknowledged irrespective of AutoAck flag
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(Prefix(AOESW, "test"), "alarm1"),
      oldSeverity = Critical,
      newSeverity = Warning,
      isAutoAcknowledgeable = true,
      oldAckStatus = Unacknowledged,
      newAckStatus = Unacknowledged
    ),
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(Prefix(AOESW, "test"), "alarm2"),
      oldSeverity = Critical,
      newSeverity = Warning,
      isAutoAcknowledgeable = true,
      oldAckStatus = Acknowledged,
      newAckStatus = Unacknowledged
    ),
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(Prefix(AOESW, "test"), "alarm3"),
      oldSeverity = Critical,
      newSeverity = Warning,
      isAutoAcknowledgeable = false,
      oldAckStatus = Acknowledged,
      newAckStatus = Unacknowledged
    ),
    // ====== Severity = Okay && AutoAck = true =====
    // AckStatus = Acknowledged
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(Prefix(AOESW, "test"), "alarm4"),
      oldSeverity = Disconnected,
      newSeverity = Okay,
      isAutoAcknowledgeable = true,
      oldAckStatus = Unacknowledged,
      newAckStatus = Acknowledged
    ),
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(Prefix(AOESW, "test"), "alarm5"),
      oldSeverity = Critical,
      newSeverity = Okay,
      isAutoAcknowledgeable = true,
      oldAckStatus = Unacknowledged,
      newAckStatus = Acknowledged
    ),
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(Prefix(AOESW, "test"), "alarm6"),
      oldSeverity = Okay,
      newSeverity = Okay,
      isAutoAcknowledgeable = true,
      oldAckStatus = Unacknowledged,
      newAckStatus = Acknowledged
    ),
    // ====== Severity = Okay && AutoAck = false =====
    // NewAckStatus = OldAckStatus
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(Prefix(AOESW, "test"), "alarm7"),
      oldSeverity = Warning,
      newSeverity = Okay,
      isAutoAcknowledgeable = false,
      oldAckStatus = Unacknowledged,
      newAckStatus = Unacknowledged
    ),
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(Prefix(AOESW, "test"), "alarm8"),
      oldSeverity = Warning,
      newSeverity = Okay,
      isAutoAcknowledgeable = false,
      oldAckStatus = Acknowledged,
      newAckStatus = Acknowledged
    )
  )

  val AckStatusTestCasesForDisconnected = List(
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(Prefix(AOESW, "test"), "alarm1"),
      oldSeverity = Okay,
      newSeverity = null,            // this won't be used, new Severity will be Disconnected when old severity expires
      isAutoAcknowledgeable = false, // this flag does not have any significance in this test
      oldAckStatus = Acknowledged,
      newAckStatus = Acknowledged
    ),
    SetSeverityAckStatusTestCase(
      alarmKey = AlarmKey(Prefix(AOESW, "test"), "alarm2"),
      oldSeverity = Warning,
      newSeverity = null,            // this won't be used, new Severity will be Disconnected when old severity expires
      isAutoAcknowledgeable = false, // this flag does not have any significance in this test
      oldAckStatus = Unacknowledged,
      newAckStatus = Unacknowledged
    )
  )
}
