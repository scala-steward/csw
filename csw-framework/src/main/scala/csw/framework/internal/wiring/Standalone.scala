package csw.framework.internal.wiring

import akka.actor.typed.ActorRef
import csw.framework.internal.configparser.ConfigParser
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.framework.models.CswContext
import csw.messages.ComponentMessage
import csw.services.logging.scaladsl.LoggerFactory

import scala.async.Async.{async, await}
import scala.concurrent.Future

/**
 * Start a supervisor actor without a container, in it's own actor system, using the component information provided in a configuration file
 */
object Standalone {

  /**
   * Spawns a component in standalone mode
   *
   * @param config represents the componentInfo data
   * @param wiring represents the class for initializing necessary instances to run a component(s)
   * @return a Future that completes with actor ref of spawned component
   */
  def spawn(
      config: com.typesafe.config.Config,
      wiring: FrameworkWiring
  ): Future[ActorRef[ComponentMessage]] = {
    import wiring._
    import actorRuntime._

    val componentInfo      = ConfigParser.parseStandalone(config)
    val cswFrameworkSystem = new CswFrameworkSystem(system)

    async {
      val eventService  = eventServiceFactory.make(locationService)
      val alarmService  = alarmServiceFactory.makeClientApi(locationService)
      val loggerFactory = new LoggerFactory(componentInfo.name)
      val cswCtx        = CswContext(locationService, eventService, alarmService, loggerFactory)

      val supervisorBehavior = SupervisorBehaviorFactory.make(
        None,
        componentInfo,
        registrationFactory,
        commandResponseManagerFactory,
        cswCtx
      )
      await(cswFrameworkSystem.spawnTyped(supervisorBehavior, componentInfo.name))
    }
  }
}
