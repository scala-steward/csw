package csw.services.location.internal

import csw.services.location.models.{ComponentType, ConnectionType}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, ObjectEncoder}

case class ConnectionInfo(name: String, componentType: ComponentType, connectionType: ConnectionType) {
  override def toString: String = s"$name-${componentType.name}-${connectionType.name}"
}

object ConnectionInfo {
  implicit val infoDecoder: Decoder[ConnectionInfo]       = deriveDecoder[ConnectionInfo]
  implicit val infoEncoder: ObjectEncoder[ConnectionInfo] = deriveEncoder[ConnectionInfo]
}
