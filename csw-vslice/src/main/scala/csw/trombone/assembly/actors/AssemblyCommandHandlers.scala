package csw.trombone.assembly.actors

import akka.typed.ActorRef
import csw.messages._
import csw.messages.ccs.commands.ControlCommand
import csw.messages.location.Connection
import csw.trombone.assembly.commands.{AssemblyCommand, AssemblyState}

trait AssemblyCommandHandlers {
  var hcds: Map[Connection, Option[ActorRef[SupervisorExternalMessage]]]
  var currentState: AssemblyState
  var currentCommand: Option[List[AssemblyCommand]]
  var tromboneStateActor: ActorRef[PubSub[AssemblyState]]

  def onNotFollowing(controlCommand: ControlCommand): AssemblyCommandState

  def onExecuting(controlCommand: ControlCommand): AssemblyCommandState
  def onExecutingCommandComplete(runId: String, result: CommandExecutionResponse): Unit
}
