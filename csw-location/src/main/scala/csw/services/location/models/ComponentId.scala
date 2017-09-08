package csw.services.location.models

import io.circe.Decoder

/**
 * Represents a component based on its name and type.
 *
 * ''Note : '' Name should not contain
 *  - leading or trailing spaces
 *  - and hyphen (-)
 */
case class ComponentId(name: String, componentType: ComponentType) extends TmtSerializable {

  def fullName: String = s"$name-${componentType.name}"

  require(name == name.trim, "component name has leading and trailing whitespaces")

  require(!name.contains("-"), "component name has '-'")
}

object ComponentId {
  import io.circe.generic.semiauto._
  implicit def decoder: Decoder[ComponentId] = deriveDecoder[ComponentId]
}
