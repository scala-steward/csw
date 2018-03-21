package csw.services.event.internal.commons

import csw.common.location.Connection.TcpConnection
import csw.common.location.{ComponentId, ComponentType}

private[csw] object EventServiceConnection {
  val value = TcpConnection(ComponentId("EventServer", ComponentType.Service))
}
