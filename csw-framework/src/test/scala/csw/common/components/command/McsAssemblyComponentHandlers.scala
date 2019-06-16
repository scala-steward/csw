package csw.common.components.command

import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.TopLevelActorMessage
import csw.common.components.command.ComponentStateForCommand.{longRunningCmdCompleted, _}
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.{AkkaLocation, TrackingEvent}
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandIssue, ControlCommand, Setup}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class McsAssemblyComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  implicit val timeout: Timeout     = 10.seconds
  implicit val scheduler: Scheduler = ctx.system.scheduler
  implicit val ec: ExecutionContext = ctx.executionContext
  var hcdComponent: CommandService  = _
  var runId: Id                     = _
  var shortSetup: Setup             = _
  var mediumSetup: Setup            = _
  var longSetup: Setup              = _

  import cswCtx._

  override def initialize(): Future[Unit] = {
    componentInfo.connections.headOption match {
      case Some(hcd) ⇒
        cswCtx.locationService.resolve(hcd.of[AkkaLocation], 5.seconds).map {
          case Some(akkaLocation) ⇒ hcdComponent = CommandServiceFactory.make(akkaLocation)(ctx.system)
          case None               ⇒ throw new RuntimeException("Could not resolve hcd location, Initialization failure.")
        }
      case None ⇒ Future.successful(Unit)
    }
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = Unit

  override def validateCommand(controlCommand: ControlCommand): ValidateCommandResponse = {
    controlCommand.commandName match {
      case `longRunning` ⇒ Accepted(controlCommand.runId)
      case `moveCmd`     ⇒ Accepted(controlCommand.runId)
      case `initCmd`     ⇒ Accepted(controlCommand.runId)
      case `invalidCmd`  ⇒ Invalid(controlCommand.runId, CommandIssue.OtherIssue("Invalid"))
      case _             ⇒ Invalid(controlCommand.runId, UnsupportedCommandIssue(controlCommand.commandName.name))
    }
  }

  override def onSubmit(controlCommand: ControlCommand): SubmitResponse = {
    controlCommand.commandName match {
      case `longRunning` ⇒
        runId = controlCommand.runId

        // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
        //#addSubCommand
        // When receiving the command, onSubmit adds three subCommands
        shortSetup = Setup(prefix, shortRunning, controlCommand.maybeObsId)
        commandResponseManager.addSubCommand(runId, shortSetup.runId)

        mediumSetup = Setup(prefix, mediumRunning, controlCommand.maybeObsId)
        commandResponseManager.addSubCommand(runId, mediumSetup.runId)

        longSetup = Setup(prefix, longRunning, controlCommand.maybeObsId)
        commandResponseManager.addSubCommand(runId, longSetup.runId)
        //#addSubCommand

        // this is to simulate that assembly is splitting command into three sub commands and forwarding same to hcd
        // longSetup takes 5 seconds to finish
        // shortSetup takes 1 second to finish
        // mediumSetup takes 3 seconds to finish
        processCommand(longSetup)
        processCommand(shortSetup)
        processCommand(mediumSetup)

        // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
        //#subscribe-to-command-response-manager
        // query the status of original command received and publish the state when its status changes to
        // Completed
        commandResponseManager
          .queryFinal(controlCommand.runId)
          .foreach {
            case Completed(_) ⇒
              currentStatePublisher.publish(
                CurrentState(controlCommand.source, StateName("testStateName"), Set(choiceKey.set(longRunningCmdCompleted)))
              )
            case _ ⇒
          }
        //#subscribe-to-command-response-manager

        //#query-command-response-manager
        // query CommandResponseManager to get the current status of Command, for example: Accepted/Completed/Invalid etc.
        commandResponseManager
          .query(controlCommand.runId)
          .map(
            _ ⇒ () // may choose to publish current state to subscribers or do other operations
          )
        // Return response
        Started(controlCommand.runId)
      //#query-command-response-manager

      case `initCmd` ⇒ Completed(controlCommand.runId)

      case `moveCmd` ⇒ Completed(controlCommand.runId)

      case _ ⇒ //do nothing
        Completed(controlCommand.runId)

    }
  }

  private def processCommand(controlCommand: ControlCommand) = {
    hcdComponent
      .submit(controlCommand)
      .map {
        // DEOPSCSW-371: Provide an API for CommandResponseManager that hides actor based interaction
        //#updateSubCommand
        // An original command is split into sub-commands and sent to a component.
        // The current state publishing is not relevant to the updateSubCommand usage.
        case _: Completed ⇒
          controlCommand.runId match {
            case id if id == shortSetup.runId ⇒
              currentStatePublisher
                .publish(CurrentState(shortSetup.source, StateName("testStateName"), Set(choiceKey.set(shortCmdCompleted))))
              // As the commands get completed, the results are updated in the commandResponseManager
              commandResponseManager.updateSubCommand(Completed(id))
            case id if id == mediumSetup.runId ⇒
              currentStatePublisher
                .publish(CurrentState(mediumSetup.source, StateName("testStateName"), Set(choiceKey.set(mediumCmdCompleted))))
              commandResponseManager.updateSubCommand(Completed(id))
            case id if id == longSetup.runId ⇒
              currentStatePublisher
                .publish(CurrentState(longSetup.source, StateName("testStateName"), Set(choiceKey.set(longCmdCompleted))))
              commandResponseManager.updateSubCommand(Completed(id))
          }
        //#updateSubCommand
        case _ ⇒ // Do nothing
      }
  }

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = Future.successful(Unit)

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
