package csw.common.framework.models

import enumeratum.{Enum, EnumEntry}
import pureconfig.ConfigConvert.catchReadError
import pureconfig.ConfigReader

import scala.collection.immutable.IndexedSeq

/**
 * Describes how a component uses the location service
 */
sealed abstract class LocationServiceUsage(override val entryName: String) extends EnumEntry {
  def name: String = entryName
}

case object DoNotRegister            extends LocationServiceUsage("DoNotRegister")
case object RegisterOnly             extends LocationServiceUsage("RegisterOnly")
case object RegisterAndTrackServices extends LocationServiceUsage("RegisterAndTrackServices")

object LocationServiceUsage extends Enum[LocationServiceUsage] {

  override def values: IndexedSeq[LocationServiceUsage] = findValues

  val JDoNotRegister: LocationServiceUsage            = DoNotRegister
  val JRegisterOnly: LocationServiceUsage             = RegisterOnly
  val JRegisterAndTrackServices: LocationServiceUsage = RegisterAndTrackServices

  implicit val reader: ConfigReader[LocationServiceUsage] =
    ConfigReader.fromString[LocationServiceUsage](catchReadError(s ⇒ from(s)))

  def from(s: String): LocationServiceUsage = s match {
    case DoNotRegister.entryName            ⇒ DoNotRegister
    case RegisterOnly.entryName             ⇒ RegisterOnly
    case RegisterAndTrackServices.entryName ⇒ RegisterAndTrackServices
    case _ ⇒
      throw new NoSuchElementException(
        s"$s is not a member of allowed values ($DoNotRegister, $RegisterOnly, $RegisterAndTrackServices)"
      )
  }
}
