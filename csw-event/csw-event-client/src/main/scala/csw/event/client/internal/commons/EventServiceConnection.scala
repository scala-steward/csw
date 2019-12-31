package csw.event.client.internal.commons

import csw.location.models.Connection.TcpConnection
import csw.location.models.{ComponentId, ComponentType}
import csw.prefix.models.Subsystem
import csw.prefix.models.Prefix

/**
 * `EventServiceConnection` is a wrapper over predefined `TcpConnection` representing event service. It is used to resolve
 * event service location for client in `EventServiceResolver`
 */
private[csw] object EventServiceConnection {
  val value = TcpConnection(ComponentId(Prefix(Subsystem.CSW, "EventServer"), ComponentType.Service))
}
