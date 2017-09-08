package csw.common.framework.models

import csw.services.location.models.{ComponentType, Connection}

import scala.collection.JavaConverters._

/**
 * The information needed to create a component
 */
final case class ComponentInfo(
    name: String,
    componentType: ComponentType,
    prefix: String,
    behaviorFactoryClassName: String,
    locationServiceUsage: LocationServiceUsage,
    connections: Set[Connection] = Set.empty
) {

  /**
   * Java API to get the list of connections for the assembly
   */
  def getConnections: java.util.List[Connection] = connections.toList.asJava
}

object ComponentInfo {
  import io.circe.generic.semiauto._
  import io.circe.{Decoder, ObjectEncoder}

  implicit val barDecoder: Decoder[ComponentInfo]       = deriveDecoder[ComponentInfo]
  implicit val barEncoder: ObjectEncoder[ComponentInfo] = deriveEncoder[ComponentInfo]
}
