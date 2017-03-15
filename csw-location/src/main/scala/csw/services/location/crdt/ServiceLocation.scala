package csw.services.location.crdt

import java.net.URI

import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import csw.services.location.common.Networks
import csw.services.location.scaladsl.models.Connection
import csw.services.location.scaladsl.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}

sealed trait ServiceLocation extends Serializable {
  def connection: Connection
  def uri: URI
}

final case class AkkaServiceLocation(connection: AkkaConnection, actorRef: ActorRef) extends ServiceLocation {
  private def actorPath = ActorPath.fromString(Serialization.serializedActorPath(actorRef))
  val uri: URI = new URI(actorPath.toString)
}

final case class HttpServiceLocation(connection: HttpConnection, port: Int, path: String) extends ServiceLocation {
  val uri: URI = new URI(s"http://${Networks.getPrimaryIpv4Address.getHostAddress}:$port/$path")
}

final case class TcpServiceLocation(connection: TcpConnection, port: Int) extends ServiceLocation {
  val uri: URI = new URI(s"tcp://${Networks.getPrimaryIpv4Address.getHostAddress}:$port")
}
