package csw.trombone.assembly.actors

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.ActorContext
import csw.messages.PubSub.CommandStatePubSub
import csw.messages.PubSub.CommandStatePubSub.Publish
import csw.trombone.assembly.AssemblyCommandHandlerMsgs.{CommandComplete, CommandMessageE}
import csw.trombone.assembly._
import csw.trombone.assembly.actors.CommandExecutionState.{Executing, Following, NotFollowing}

class AssemblyFollowingCommandBehavior(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    assemblyContext: AssemblyContext,
    pubSubCommandState: ActorRef[CommandStatePubSub],
    assemblyCommandHandlers: AssemblyFollowingCommandHandlers
) extends AssemblyCommandBehavior(ctx, assemblyContext, pubSubCommandState, assemblyCommandHandlers) {

  override def onMessage(msg: AssemblyCommandHandlerMsgs): Behavior[AssemblyCommandHandlerMsgs] = {
    (commandExecutionState, msg) match {
      case (_, msg: CommonMsgs)                  ⇒ onCommon(msg)
      case (NotFollowing, msg: NotFollowingMsgs) ⇒ onNotFollowing(msg)
      case (Following, msg: FollowingMsgs)       ⇒ onFollowing(msg)
      case (Executing, msg: ExecutingMsgs)       ⇒ onExecuting(msg)
      case _                                     ⇒ println(s"Unexpected message :[$msg] received by component in lifecycle state :[$commandExecutionState]")
    }
    this
  }

  def onFollowing(msg: FollowingMsgs): Unit = msg match {
    case CommandMessageE(runId, controlCommand) =>
      val assemblyCommandState = assemblyCommandHandlers.onFollowing(controlCommand)
      assemblyCommandState.commandOrResponse match {
        case Left(commands)           => commands.foreach(executeCommand)
        case Right(executionResponse) => pubSubCommandState ! Publish(runId, executionResponse)
      }

      commandExecutionState = assemblyCommandState.commandExecutionState
    case CommandComplete(result) =>
      assemblyCommandHandlers.onFollowingCommandComplete(result)
  }
}
