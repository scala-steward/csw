package csw.location.agent.wiring

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.{ActorSystem, CoordinatedShutdown, Scheduler}
import akka.stream.{ActorMaterializer, Materializer}
import csw.logging.core.internal.LoggingSystem
import csw.logging.core.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import csw.services.BuildInfo

import scala.concurrent.{ExecutionContextExecutor, Future}

private[agent] class ActorRuntime(_actorSystem: ActorSystem) {
  implicit val system: ActorSystem          = _actorSystem
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()
  implicit val scheduler: Scheduler         = system.scheduler

  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(system)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, system)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}