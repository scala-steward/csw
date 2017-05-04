package csw.services.csclient.cli

import akka.actor.CoordinatedShutdown
import csw.services.config.api.scaladsl.ConfigAdminService
import csw.services.config.client.internal.ActorRuntime
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

class ClientCliWiring(clusterSettings: ClusterSettings) {
  lazy val actorRuntime                     = new ActorRuntime()
  lazy val locationService: LocationService = LocationServiceFactory.withSettings(clusterSettings)
  lazy val configAdminService: ConfigAdminService =
    ConfigClientFactory.makeAdmin(actorRuntime.actorSystem, locationService)
  lazy val commandLineRunner = new CommandLineRunner(configAdminService, actorRuntime)

  actorRuntime.coordinatedShutdown.addTask(
    CoordinatedShutdown.PhaseBeforeServiceUnbind,
    "location-service-shutdown"
  )(() ⇒ locationService.shutdown())
}
