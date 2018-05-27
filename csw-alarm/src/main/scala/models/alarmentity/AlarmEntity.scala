package models.alarmentity

import models.Severity

case class AlarmEntity(
    subsystem: String,
    component: String,
    name: String,
    description: String,
    location: String,
    alarmType: AlarmType,
    severityLevels: List[Severity],
    probableCause: String,
    operatorResponse: String,
    acknowledge: Boolean,
    latched: Boolean
)
