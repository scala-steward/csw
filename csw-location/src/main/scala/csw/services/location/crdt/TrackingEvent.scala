package csw.services.location.crdt

import csw.services.location.scaladsl.models.Connection

sealed trait TrackingEvent {
  def connection: Connection
}

case class Updated(serviceLocation: ServiceLocation) extends TrackingEvent {
  override def connection: Connection = serviceLocation.connection
}
case class Deleted(connection: Connection) extends TrackingEvent
