package csw.trombone.assembly.actors

import akka.actor.Scheduler
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.framework.internal.pubsub.PubSubBehavior
import csw.messages.PubSub.Publish
import csw.messages._
import csw.messages.ccs.ValidationIssue.{
  UnsupportedCommandInStateIssue,
  UnsupportedCommandIssue,
  WrongInternalStateIssue,
  WrongPrefixIssue
}
import csw.messages.ccs.commands.{ControlCommand, Setup}
import csw.messages.location.Connection
import csw.trombone.assembly._
import csw.trombone.assembly.commands._

import scala.concurrent.duration.DurationInt

class TromboneAssemblyCommandBehaviorFactory extends AssemblyCommandBehaviorFactory {
  override protected def assemblyCommandHandlers(
      ctx: ActorContext[AssemblyCommandHandlerMsgs],
      ac: AssemblyContext,
      tromboneHCDs: Map[Connection, Option[ActorRef[SupervisorExternalMessage]]],
      allEventPublisher: Option[ActorRef[TrombonePublisherMsg]]
  ): TromboneCommandHandler =
    new TromboneCommandHandler(ctx, ac, tromboneHCDs, allEventPublisher)
}

class TromboneCommandHandler(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    ac: AssemblyContext,
    tromboneHCDs: Map[Connection, Option[ActorRef[SupervisorExternalMessage]]],
    allEventPublisher: Option[ActorRef[TrombonePublisherMsg]]
) extends AssemblyFollowingCommandHandlers {

  implicit val scheduler: Scheduler = ctx.system.scheduler
  import TromboneState._
  import ac._
  implicit val system: ActorSystem[Nothing] = ctx.system
  implicit val timeout: Timeout             = Timeout(5.seconds)

  private var setElevationItem                                    = naElevation(calculationConfig.defaultInitialElevation)
  private var followCommandActor: ActorRef[FollowCommandMessages] = _

  override var hcds: Map[Connection, Option[ActorRef[SupervisorExternalMessage]]] = tromboneHCDs
  override var currentState: AssemblyState                                        = defaultTromboneState
  override var currentCommand: Option[List[AssemblyCommand]]                      = _
  override var tromboneStateActor: ActorRef[PubSub[AssemblyState]] =
    ctx.spawnAnonymous(Actor.mutable[PubSub[AssemblyState]](ctx ⇒ new PubSubBehavior(ctx, "")))

  override def onNotFollowing(controlCommand: ControlCommand): AssemblyCommandState = controlCommand match {
    case s: Setup =>
      s.prefix match {
        case ac.initCK =>
          AssemblyCommandState(Right(Completed("")), CommandExecutionState.NotFollowing)

        case ac.datumCK =>
          AssemblyCommandState(
            Left(
              List(
                new DatumCommand(ctx,
                                 "",
                                 ac,
                                 s,
                                 hcds.head._2,
                                 currentState.asInstanceOf[TromboneState],
                                 tromboneStateActor)
              )
            ),
            CommandExecutionState.Executing
          )
        case ac.moveCK =>
          AssemblyCommandState(
            Left(
              List(
                new MoveCommand(ctx,
                                "",
                                ac,
                                s,
                                hcds.head._2,
                                currentState.asInstanceOf[TromboneState],
                                tromboneStateActor)
              )
            ),
            CommandExecutionState.Executing
          )
        case ac.positionCK =>
          AssemblyCommandState(
            Left(
              List(
                new PositionCommand(ctx,
                                    "",
                                    ac,
                                    s,
                                    hcds.head._2,
                                    currentState.asInstanceOf[TromboneState],
                                    tromboneStateActor)
              )
            ),
            CommandExecutionState.Executing
          )
        case ac.setElevationCK =>
          setElevationItem = s(ac.naElevationKey)
          AssemblyCommandState(
            Left(
              List(
                new SetElevationCommand(ctx,
                                        "",
                                        ac,
                                        s,
                                        hcds.head._2,
                                        currentState.asInstanceOf[TromboneState],
                                        tromboneStateActor)
              )
            ),
            CommandExecutionState.Executing
          )

        case ac.followCK =>
          val nssItem = s(ac.nssInUseKey)
          followCommandActor = ctx.spawnAnonymous(
            FollowCommandActor.make(ac, setElevationItem, nssItem, hcds.head._2, allEventPublisher)
          )
          AssemblyCommandState(
            Left(
              List(
                new FollowCommand(ctx,
                                  "",
                                  ac,
                                  s,
                                  hcds.head._2,
                                  currentState.asInstanceOf[TromboneState],
                                  tromboneStateActor)
              )
            ),
            CommandExecutionState.Following
          )

        case ac.stopCK =>
          val commandResponse =
            NoLongerValid("", WrongInternalStateIssue("Trombone assembly must be executing a command to use stop"))
          AssemblyCommandState(Right(commandResponse), CommandExecutionState.NotFollowing)

        case ac.setAngleCK =>
          val commandResponse =
            NoLongerValid("", WrongInternalStateIssue("Trombone assembly must be following for setAngle"))
          AssemblyCommandState(Right(commandResponse), CommandExecutionState.NotFollowing)

        case otherCommand =>
          val commandResponse = NoLongerValid(
            "",
            UnsupportedCommandInStateIssue(
              s"""Trombone assembly does not support the command \"${otherCommand.prefix}\" in the current state."""
            )
          )
          AssemblyCommandState(Right(commandResponse), CommandExecutionState.NotFollowing)
      }
    case _ ⇒
      val commandResponse =
        NoLongerValid("", UnsupportedCommandIssue(s"Unexpected command :[$controlCommand] received by component"))
      AssemblyCommandState(Right(commandResponse), CommandExecutionState.NotFollowing)
  }

  override def onFollowing(controlCommand: ControlCommand): AssemblyCommandState = controlCommand match {
    case s: Setup =>
      s.prefix match {
        case ac.datumCK | ac.moveCK | ac.positionCK | ac.followCK | ac.setElevationCK =>
          val commandResponse = NoLongerValid(
            "",
            WrongInternalStateIssue(
              "Trombone assembly cannot be following for datum, move, position, setElevation, and follow"
            )
          )
          AssemblyCommandState(Right(commandResponse), CommandExecutionState.Following)

        case ac.setAngleCK =>
          AssemblyCommandState(
            Left(
              List(
                new SetAngleCommand(ctx,
                                    "",
                                    ac,
                                    s,
                                    followCommandActor,
                                    hcds.head._2,
                                    currentState.asInstanceOf[TromboneState],
                                    tromboneStateActor)
              )
            ),
            CommandExecutionState.Following
          )

        case ac.stopCK =>
          currentCommand.foreach(x ⇒ x.foreach(_.stopCommand()))
          tromboneStateActor ! Publish(
            TromboneState(cmdItem(cmdReady),
                          moveItem(moveIndexed),
                          currentState.asInstanceOf[TromboneState].sodiumLayer,
                          currentState.asInstanceOf[TromboneState].nss)
          )
          ctx.stop(followCommandActor)
          AssemblyCommandState(Right(Completed("")), CommandExecutionState.NotFollowing)

        case other =>
          val commandResponse = Invalid("", WrongPrefixIssue(s"Unknown config key: $controlCommand"))
          AssemblyCommandState(Right(commandResponse), CommandExecutionState.Following)
      }
    case _ ⇒
      val commandResponse =
        NoLongerValid("", UnsupportedCommandIssue(s"Unexpected command :[$controlCommand] received by component"))
      AssemblyCommandState(Right(commandResponse), CommandExecutionState.NotFollowing)
  }

  override def onExecuting(controlCommand: ControlCommand): AssemblyCommandState = controlCommand match {
    case Setup(ac.commandInfo, ac.stopCK, _) =>
      currentCommand.foreach(x ⇒ x.foreach(_.stopCommand()))
      AssemblyCommandState(Right(Cancelled("")), CommandExecutionState.NotFollowing)

    case x =>
      val commandResponse =
        NoLongerValid("", UnsupportedCommandIssue(s"Unexpected command :[$controlCommand] received by component"))
      AssemblyCommandState(Right(commandResponse), CommandExecutionState.Executing)
  }

  override def onFollowingCommandComplete(runId: String, result: CommandExecutionResponse): Unit = {}

  override def onExecutingCommandComplete(runId: String, result: CommandExecutionResponse): Unit = {}

}
