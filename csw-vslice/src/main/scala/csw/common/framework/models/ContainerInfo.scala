package csw.common.framework.models

final case class ContainerInfo(
    name: String,
    locationServiceUsage: LocationServiceUsage,
    components: Set[ComponentInfo]
) {
  require(!components.isEmpty, "components can not be empty.")
}
