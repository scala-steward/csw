package csw.framework.scaladsl

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.messages.PubSub.{CommandStatePubSub, PublisherMessage}
import csw.messages.RunningMessage.DomainMessage
import csw.messages.ccs.Validation
import csw.messages.ccs.Validations.Valid
import csw.messages.ccs.commands.ControlCommand
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.params.states.CurrentState
import csw.messages.{CommandMessage, CommandResponse, ComponentMessage}
import csw.services.location.scaladsl.LocationService

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class ComponentHandlers[Msg <: DomainMessage: ClassTag](
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    pubSubCommandState: ActorRef[CommandStatePubSub],
    locationService: LocationService
) {

  var isOnline: Boolean = false

  def initialize(): Future[Unit]

  def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit
  def onDomainMsg(msg: Msg): Unit
  def onSubmit(controlCommand: ControlCommand, replyTo: ActorRef[CommandResponse]): Validation
  def onOneway(controlCommand: ControlCommand): Validation
  def onCommand(runId: String, command: ControlCommand, replyTo: ActorRef[CommandResponse]): Validation = Valid
  def onValidCommand(runId: String, controlCommand: ControlCommand): Unit                               = {}
  def onShutdown(): Future[Unit]
  def onGoOffline(): Unit
  def onGoOnline(): Unit
}
