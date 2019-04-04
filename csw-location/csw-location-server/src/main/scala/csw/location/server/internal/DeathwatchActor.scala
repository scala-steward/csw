package csw.location.server.internal

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.cluster.ddata.typed.scaladsl.Replicator
import akka.cluster.ddata.typed.scaladsl.Replicator.Changed
import csw.location.api.models.{AkkaLocation, Location}
import csw.location.api.scaladsl.LocationService
import csw.location.server.commons.{CswCluster, LocationServiceLogger}
import csw.location.server.internal.Registry.AllServices
import csw.logging.api.scaladsl.Logger

/**
 * DeathWatchActor tracks the health of all components registered with LocationService.
 *
 * @param locationService is used to unregister Actors that are no more alive
 */
private[location] class DeathwatchActor(locationService: LocationService, hostname: String) {
  import DeathwatchActor.Msg

  /**
   * Deathwatch behavior processes `DeathwatchActor.Msg` type events sent by replicator for newly registered Locations.
   * Terminated signal will be received upon termination of an actor that was being watched.
   *
   * @see [[akka.actor.Terminated]]
   */
  private[location] def behavior(watchedLocations: LocalAkkaLocations): Behavior[Msg] =
    Behaviors.setup { ctx ⇒
      val log: Logger = LocationServiceLogger.getLogger(ctx)

      Behaviors.receiveMessage[Msg] { changeMsg ⇒
        val allLocalAkkaLocations   = LocalAkkaLocations(changeMsg.get(AllServices.Key).entries.values.toSet, hostname)
        val unwatchedLocalLocations = allLocalAkkaLocations diff watchedLocations

        unwatchedLocalLocations.all.foreach { akkaLocation ⇒
          log.debug(s"Started watching actor: ${akkaLocation.actorRef.toString}")
          ctx.watch(akkaLocation.actorRef)
        }

        //all local Actor locations (AkkaLocations) are now watched
        behavior(watchedLocations union unwatchedLocalLocations)

      } receiveSignal {
        case (_, Terminated(deadActorRef)) ⇒
          log.warn(s"Un-watching terminated actor: ${deadActorRef.toString}")
          //stop watching the terminated actor
          ctx.unwatch(deadActorRef)
          //Unregister the dead location and remove it from the list of watched locations

          val maybeLocation = watchedLocations.locationOf(deadActorRef)
          maybeLocation.map(location ⇒ locationService.unregister(location.connection))
          behavior(watchedLocations.remove(maybeLocation))
      }
    }
}

private[location] object DeathwatchActor {

  private val log: Logger = LocationServiceLogger.getLogger

  import akka.actor.typed.scaladsl.adapter._
  //message type handled by the for the typed deathwatch actor
  type Msg = Changed[AllServices.Value]

  /**
   * Start the DeathwatchActor using the given locationService
   *
   * @param cswCluster is used to get remote ActorSystem to create DeathwatchActor
   */
  def start(cswCluster: CswCluster, locationService: LocationService): ActorRef[Msg] = {
    log.debug("Starting Deathwatch actor")
    val actorRef = cswCluster.actorSystem.spawn(
      //span the actor with empty set of watched locations
      new DeathwatchActor(locationService, cswCluster.hostname).behavior(LocalAkkaLocations.empty),
      name = "location-service-death-watch-actor"
    )

    //Subscribed to replicator to get events for locations registered with LocationService
    cswCluster.replicator ! Replicator.Subscribe(AllServices.Key, actorRef)
    actorRef
  }
}
