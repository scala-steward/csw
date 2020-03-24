package csw.alarm.models

import csw.alarm.models.AlarmType._

// DEOPSCSW-438: Model to represent Alarm type values
class AlarmTypeTest extends EnumTest(AlarmType, "| DEOPSCSW-438") {
  override val expectedValues = Set(
    Absolute,
    BitPattern,
    Calculated,
    Deviation,
    Discrepancy,
    Instrument,
    RateChange,
    RecipeDriven,
    Safety,
    Statistical,
    System
  )
}
