package csw.trombone.assembly.actors

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.CommandMessage.Submit
import csw.messages.PubSub.PublisherMessage
import csw.messages._
import csw.messages.ccs.Validations.Valid
import csw.messages.ccs.commands.{ControlCommand, Observe, Setup}
import csw.messages.ccs.{Validation, Validations}
import csw.messages.framework.ComponentInfo
import csw.messages.location._
import csw.messages.params.states.CurrentState
import csw.services.location.scaladsl.LocationService
import csw.trombone.assembly.AssemblyCommandHandlerMsgs.CommandMessageE
import csw.trombone.assembly.AssemblyContext.{TromboneCalculationConfig, TromboneControlConfig}
import csw.trombone.assembly.CommonMsgs.UpdateHcdLocations
import csw.trombone.assembly.DiagPublisherMessages.{DiagnosticState, OperationsState}
import csw.trombone.assembly.ParamValidation._
import csw.trombone.assembly._

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class TromboneAssemblyBehaviorFactory extends ComponentBehaviorFactory[DiagPublisherMessages] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PublisherMessage[CurrentState]],
      locationService: LocationService
  ): ComponentHandlers[DiagPublisherMessages] =
    TromboneAssemblyHandlers(ctx, componentInfo, pubSubRef, locationService, None, None, Map.empty)
}

case class TromboneAssemblyHandlers(
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService,
    diagPublisher: Option[ActorRef[DiagPublisherMessages]],
    commandHandler: Option[ActorRef[AssemblyCommandHandlerMsgs]],
    runningHcds: Map[Connection, Option[ActorRef[SupervisorExternalMessage]]]
) extends ComponentHandlers[DiagPublisherMessages](ctx, componentInfo, pubSubRef, locationService) {

  implicit var ac: AssemblyContext  = _
  implicit val ec: ExecutionContext = ctx.executionContext

  def onRun(): Future[Unit] = Future.unit

  def initialize(): Future[ComponentHandlers[DiagPublisherMessages]] = async {
    val (calculationConfig, controlConfig) = await(getAssemblyConfigs)
    ac = AssemblyContext(componentInfo.asInstanceOf[ComponentInfo], calculationConfig, controlConfig)

    val eventPublisher = ctx.spawnAnonymous(TrombonePublisher.make(ac))

    this.copy(
      diagPublisher = Some(ctx.spawnAnonymous(DiagPublisher.make(ac, runningHcds.head._2, Some(eventPublisher)))),
      commandHandler = Some(
        ctx.spawnAnonymous(new TromboneAssemblyCommandBehaviorFactory().make(ac, runningHcds, Some(eventPublisher)))
      ),
      runningHcds = Map.empty
    )
  }

  override def onShutdown(): Future[Unit] = {
    Future.successful(println("Received Shutdown"))
  }

  override def onGoOffline(): Unit = println("Received running offline")

  override def onGoOnline(): Unit = println("Received GoOnline")

  def onDomainMsg(mode: DiagPublisherMessages): Unit = mode match {
    case (DiagnosticState | OperationsState) => diagPublisher.foreach(_ ! mode)
    case _                                   ⇒
  }

  override def onSubmit(controlCommand: ControlCommand, replyTo: ActorRef[CommandResponse]): Validation = {
    val validation = controlCommand match {
      case Setup(info, prefix, paramSet)   => validateOneSetup(controlCommand.asInstanceOf[Setup])
      case Observe(info, prefix, paramSet) => Valid
    }
    if (validation == Valid) {
      commandHandler.foreach(_ ! CommandMessageE(Submit(controlCommand, replyTo)))
    }
    validation
  }

  override def onOneway(controlCommand: ControlCommand): Validation = Validations.Valid

  private def getAssemblyConfigs: Future[(TromboneCalculationConfig, TromboneControlConfig)] = ???

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): ComponentHandlers[DiagPublisherMessages] = {
    val updatedRunningHcds = trackingEvent match {
      case LocationUpdated(location) =>
        runningHcds + (location.connection → Some(
          location.asInstanceOf[AkkaLocation].typedRef[SupervisorExternalMessage]
        ))
      case LocationRemoved(connection) =>
        runningHcds + (connection → None)
    }
    commandHandler.foreach(_ ! UpdateHcdLocations(updatedRunningHcds))
    this.copy(runningHcds = updatedRunningHcds)
  }
}
