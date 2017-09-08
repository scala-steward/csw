package csw.common.framework.models

final case class ContainerInfo(
    name: String,
    locationServiceUsage: LocationServiceUsage,
    components: Set[ComponentInfo]
) {
  require(!components.isEmpty, "components can not be empty.")
}

object ContainerInfo {

  import io.circe.{Decoder, ObjectEncoder}
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

  implicit val barDecoder: Decoder[ContainerInfo]       = deriveDecoder[ContainerInfo]
  implicit val barEncoder: ObjectEncoder[ContainerInfo] = deriveEncoder[ContainerInfo]
}
