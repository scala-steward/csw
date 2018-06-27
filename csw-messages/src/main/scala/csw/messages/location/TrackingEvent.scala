package csw.messages.location

import ai.x.play.json.Jsonx
import akka.actor.ActorSystem
import play.api.libs.json.Format

/**
 * TrackingEvent is used to represent location events while tracking the connection
 */
sealed abstract class TrackingEvent {

  /**
   * The connection for which this TrackingEvent is created
   */
  def connection: Connection
}

object TrackingEvent {
  implicit def trackingEventFormat(implicit actorSystem: ActorSystem): Format[TrackingEvent] = Jsonx.formatSealed[TrackingEvent]
  implicit def updatedTrackingEventFormat(implicit actorSystem: ActorSystem): Format[LocationUpdated] =
    Jsonx.formatCaseClass[LocationUpdated]
  implicit def removedTrackingEventFormat(implicit actorSystem: ActorSystem): Format[LocationRemoved] =
    Jsonx.formatCaseClass[LocationRemoved]
}

/**
 * This event represents modification in location details
 *
 * @param location the updated location for the tracked connection
 */
case class LocationUpdated(location: Location) extends TrackingEvent {

  /**
   * The connection for which this TrackingEvent is created
   */
  override def connection: Connection = location.connection
}

/**
 * This event represents unavailability of a location
 *
 * @param connection for which the location no longer exists
 */
case class LocationRemoved(connection: Connection) extends TrackingEvent
