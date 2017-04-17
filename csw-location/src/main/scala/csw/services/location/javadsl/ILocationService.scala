package csw.services.location.javadsl

import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.{util ⇒ ju}

import akka.Done
import akka.stream.KillSwitch
import akka.stream.javadsl.Source
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

import scala.concurrent.duration.FiniteDuration

/**
  * A LocationService interface to manage connections and their registrations. All operations are non-blocking.
  */
trait ILocationService {

  /**
    * Registers a connection to location
    *
    * @return A CompletableFuture which completes with Registration result
    */
  def register(registration: Registration): CompletableFuture[IRegistrationResult]

  /**
    * Unregisters the connection
    *
    * @param connection An already registered connection
    * @return A CompletableFuture which completes after un-registration happens successfully and fails otherwise
    */
  def unregister(connection: Connection): CompletableFuture[Done]

  /**
    * Unregisters all connections registered
    * ''Note: '' It is highly recommended to use this method for testing purpose only
    *
    * @return A CompletableFuture which completes after all connections are unregistered successfully or fails otherwise
    */
  def unregisterAll(): CompletableFuture[Done]

  /**
    * Resolve the location for a connection from the local cache
    *
    * @param connection A connection to resolve to with its registered location
    * @return A CompletableFuture which completes with the resolved location if found or Empty otherwise.
    */
  def find(connection: Connection): CompletableFuture[Optional[Location]]

  /**
  * Resolves the location based on the given connection
  *
  * @return A CompletableFuture which completes with the resolved location if found or None otherwise.
  */
  def resolve(connection: Connection, within: FiniteDuration): CompletableFuture[Optional[Location]]

  /**
    * Lists all locations registered
    *
    * @return A CompletableFuture which completes with a List of all registered locations
    */
  def list: CompletableFuture[ju.List[Location]]

  /**
    * Filters all locations registered based on a component type
    *
    * @return A CompletableFuture which completes with filtered locations
    */
  def list(componentType: ComponentType): CompletableFuture[ju.List[Location]]

  /**
    * Filters all locations registered based on a hostname
    *
    * @return A CompletableFuture which completes with filtered locations
    */
  def list(hostname: String): CompletableFuture[ju.List[Location]]

  /**
    * Filters all locations registered based on a connection type
    *
    * @return A CompletableFuture which completes with filtered locations
    */
  def list(connectionType: ConnectionType): CompletableFuture[ju.List[Location]]

  /**
    * Tracks the connection and send events for modification or removal of its location
    *
    * @return A stream that emits events related to the connection. It can be cancelled using KillSwitch. This will stop giving
    *         events for earlier tracked connection
    */
  def track(connection: Connection): Source[TrackingEvent, KillSwitch]

  /**
    * Shuts down the LocationService
    *
    * ''Note : '' It is recommended not to perform any operation on LocationService after calling this method
    *
    *''See Also: '' terminate method in [[csw.services.location.commons.CswCluster]]
    * @return A CompletableFuture which completes when the location service has shutdown successfully
    */
  def shutdown(): CompletableFuture[Done]

  /**
    * Returns the Scala API for this instance of location service
    */
  def asScala: LocationService
}
