package csw.services.alarms.models

import csw.services.alarms.models.AlarmEntity.FieldNames

case class AlarmEntity(
    subsystem: String,
    component: String,
    name: String,
    description: String,
    location: String,
    alarmType: AlarmEntity.AlarmType,
    severityLevels: List[AlarmEntity.SeverityLevel],
    probableCause: String,
    operatorResponse: String,
    acknowledge: Boolean,
    latched: Boolean
) {

  def asMap(): Map[String, String] = {
    Map(
      FieldNames.subsystem        → subsystem,
      FieldNames.component        → component,
      FieldNames.name             → name,
      FieldNames.description      → description,
      FieldNames.location         → location,
      FieldNames.alarmType        → alarmType.toString,
      FieldNames.severityLevels   → severityLevels.mkString(":"),
      FieldNames.probableCause    → probableCause,
      FieldNames.operatorResponse → operatorResponse,
      FieldNames.acknowledge      → acknowledge.toString,
      FieldNames.latched          → latched.toString
    )
  }
}

object AlarmEntity {

  def apply(map: Map[String, String]): Option[AlarmEntity] = {
    if (map.isEmpty) None
    else {

      val subsystem        = map(FieldNames.subsystem)
      val component        = map(FieldNames.component)
      val name             = map(FieldNames.name)
      val description      = map(FieldNames.description)
      val location         = map(FieldNames.location)
      val alarmType        = AlarmType(map(FieldNames.alarmType))
      val probableCause    = map(FieldNames.probableCause)
      val operatorResponse = map(FieldNames.operatorResponse)
      val acknowledge      = map(FieldNames.acknowledge).toBoolean
      val latched          = map(FieldNames.latched).toBoolean

      val severityLevels = map(FieldNames.severityLevels)
        .split(":")
        .toList
        .map(SeverityLevel(_).getOrElse(SeverityLevel.Disconnected))

      Some(
        AlarmEntity(subsystem,
                    component,
                    name,
                    description,
                    location,
                    alarmType,
                    severityLevels,
                    probableCause,
                    operatorResponse,
                    acknowledge,
                    latched)
      )
    }
  }

  // Field name constants
  private[alarms] object FieldNames {
    val subsystem        = "subsystem"
    val component        = "component"
    val name             = "name"
    val description      = "description"
    val location         = "location"
    val alarmType        = "alarmType"
    val severityLevels   = "severityLevels"
    val probableCause    = "probableCause"
    val operatorResponse = "operatorResponse"
    val acknowledge      = "acknowledge"
    val latched          = "latched"
  }

  sealed trait AlarmType
  case object AlarmType {

    case object Absolute     extends AlarmType
    case object BitPattern   extends AlarmType
    case object Calculated   extends AlarmType
    case object Deviation    extends AlarmType
    case object Discrepancy  extends AlarmType
    case object Instrument   extends AlarmType
    case object RateChange   extends AlarmType
    case object RecipeDriven extends AlarmType
    case object Safety       extends AlarmType
    case object Statistical  extends AlarmType
    case object System       extends AlarmType

    def apply(name: String): AlarmType = name match {
      case "Absolute"     ⇒ Absolute
      case "BitPattern"   ⇒ BitPattern
      case "Calculated"   ⇒ Calculated
      case "Deviation"    ⇒ Deviation
      case "Discrepancy"  ⇒ Discrepancy
      case "Instrument"   ⇒ Instrument
      case "RateChange"   ⇒ RateChange
      case "RecipeDriven" ⇒ RecipeDriven
      case "Safety"       ⇒ Safety
      case "Statistical"  ⇒ Statistical
      case "System"       ⇒ System
    }
  }

  sealed trait SeverityLevel {

    val level: Int

    def isAlarm: Boolean = level > 0 // XXX Disconnected and Indeterminate are not latched (for now)

    def name: String

    override def toString: String = name

  }
  object SeverityLevel {

    abstract class SeverityLevelBase(override val level: Int, override val name: String) extends SeverityLevel

    case object Disconnected  extends SeverityLevelBase(-2, "Disconnected")
    case object Indeterminate extends SeverityLevelBase(-1, "Indeterminate")
    case object Okay          extends SeverityLevelBase(0, "Okay")
    case object Warning       extends SeverityLevelBase(1, "Warning")
    case object Major         extends SeverityLevelBase(2, "Major")
    case object Critical      extends SeverityLevelBase(3, "Critical")

    def apply(name: String): Option[SeverityLevel] = name match {
      case Disconnected.name  ⇒ Some(Disconnected)
      case Indeterminate.name ⇒ Some(Indeterminate)
      case Okay.name          ⇒ Some(Okay)
      case Warning.name       ⇒ Some(Warning)
      case Major.name         ⇒ Some(Major)
      case Critical.name      ⇒ Some(Critical)
      case _                  ⇒ None
    }
  }

  case class CurrentSeverity(reported: SeverityLevel, latched: SeverityLevel)
}
