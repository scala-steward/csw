package csw.apps.clusterseed.components

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.CommandValidationResponse.Accepted
import csw.messages.PubSub.PublisherMessage
import csw.messages.RunningMessage.DomainMessage
import csw.messages._
import csw.messages.ccs.commands.ControlCommand
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.params.states.CurrentState
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.ComponentLogger

import scala.concurrent.{ExecutionContextExecutor, Future}

case class StartLogging() extends DomainMessage

class GalilComponentHandlers(
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService
) extends ComponentHandlers[StartLogging](ctx, componentInfo, pubSubRef, locationService)
    with ComponentLogger.Simple {
  implicit val ec: ExecutionContextExecutor                          = ctx.executionContext
  override def initialize(): Future[ComponentHandlers[StartLogging]] = Future(this)

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): ComponentHandlers[StartLogging] = this

  override def onDomainMsg(msg: StartLogging): ComponentHandlers[StartLogging] = {
    log.trace("Level is trace")
    log.debug("Level is debug")
    log.info("Level is info")
    log.warn("Level is warn")
    log.error("Level is error")
    log.fatal("Level is fatal")
    this
  }

  override def onSubmit(
      controlCommand: ControlCommand,
      replyTo: ActorRef[CommandResponse]
  ): (ComponentHandlers[StartLogging], CommandValidationResponse) =
    (this, Accepted(controlCommand.runId))
  override def onOneway(controlCommand: ControlCommand): (ComponentHandlers[StartLogging], CommandValidationResponse) =
    (this, Accepted(controlCommand.runId))

  override def onShutdown(): Future[Unit] = Future.successful(())

  override def onGoOffline(): ComponentHandlers[StartLogging] = this

  override def onGoOnline(): ComponentHandlers[StartLogging] = this

  override protected def componentName(): String = componentInfo.name
}
