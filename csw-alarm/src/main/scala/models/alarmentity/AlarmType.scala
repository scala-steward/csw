package models.alarmentity

import csw.messages.TMTSerializable
import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import scala.collection.immutable.IndexedSeq

sealed abstract class AlarmType() extends EnumEntry with Lowercase with TMTSerializable {
  def name: String = entryName
}

object AlarmType extends Enum[AlarmType] with PlayJsonEnum[AlarmType] {
  def values: IndexedSeq[AlarmType] = findValues

  case object Absolute extends AlarmType

  case object BitPattern extends AlarmType

  case object Calculated extends AlarmType

  case object Deviation extends AlarmType

  case object Discrepancy extends AlarmType

  case object Instrument extends AlarmType

  case object RateChange extends AlarmType

  case object RecipeDriven extends AlarmType

  case object Safety extends AlarmType

  case object Statistical extends AlarmType

  case object System extends AlarmType

}
