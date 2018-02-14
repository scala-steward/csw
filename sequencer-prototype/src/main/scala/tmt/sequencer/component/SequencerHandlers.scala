package tmt.sequencer.component

import java.nio.file.Paths

import akka.stream.ActorMaterializer
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.TypedActorSystemOps
import akka.util.Timeout
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.models.PubSub.PublisherMessage
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, TopLevelActorMessage}
import csw.services.config.api.models.ConfigData
import csw.services.config.api.scaladsl.ConfigClientService
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory
import tmt.sequencer.engine.EngineBehaviour.EngineAction
import tmt.sequencer.engine.{Engine, EngineBehaviour}

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContextExecutor, Future}

class SequencerHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers(
      ctx,
      componentInfo,
      commandResponseManager,
      pubSubRef,
      locationService,
      loggerFactory: LoggerFactory
    ) {

  implicit val ct: ActorContext[TopLevelActorMessage] = ctx
  implicit val ec: ExecutionContextExecutor           = ctx.executionContext
  implicit val mat: ActorMaterializer                 = ActorMaterializer()(ctx.system.toUntyped)

  override def initialize(): Future[Unit] = async {

    val configClient: ConfigClientService = ConfigClientFactory.clientApi(ctx.system.toUntyped, locationService)

    val maybeData: Option[ConfigData] = await(configClient.getActive(Paths.get(s"/${componentInfo.name}.sc")))

    val outPutFilePath = await {
      maybeData match {
        case Some(configData) => configData.toPath(Paths.get(""))
        case None             => ??? // TODO
      }
    }

    implicit lazy val timeout: Timeout = Timeout(5.seconds)

    val engineActor: ActorRef[EngineAction] = await(ctx.system.systemActorOf(EngineBehaviour.behaviour, "engine"))

    val engine = new Engine(engineActor, ctx.system) //TODO: what to do if engine dies ? Decide in handlers.

    // Load script
    val params = Array("scripts/ocs-sequencer.sc")
    // Run script using ammonite
    ammonite.Main.main0(params.toList, System.in, System.out, System.err)

//    implicit lazy val timeout: Timeout = Timeout(5.seconds)

//    lazy val engineActor: ActorRef[EngineAction] =
//      Await.result(ctx.system.systemActorOf(EngineBehaviour.behaviour, "engine"), timeout.duration)
//    lazy val engine = new Engine(engineActor, ctx.system)
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = ???

  override def onSubmit(controlCommand: ControlCommand): Unit = ???

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = ??? //TODO: clean up engine and other relevant instances

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
