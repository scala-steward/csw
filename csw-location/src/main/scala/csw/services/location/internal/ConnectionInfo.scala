package csw.services.location.internal

import csw.services.location.models.{ComponentType, ConnectionType}
import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}

private[location] case class ConnectionInfo(
    name: String,
    componentType: ComponentType,
    connectionType: ConnectionType
) {
  override def toString: String = s"$name-${componentType.name}-${connectionType.name}"
}

private[location] object ConnectionInfo {
  implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))
}
