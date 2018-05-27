package models

case class AlarmInfo(
    subsystem: String,
    component: String,
    name: String,
    description: String,
    location: String,
    alarmType: AlarmInfo,
    severityLevels: List[Severity],
    probableCause: String,
    operatorResponse: String,
    acknowledge: Boolean,
    latched: Boolean
)
