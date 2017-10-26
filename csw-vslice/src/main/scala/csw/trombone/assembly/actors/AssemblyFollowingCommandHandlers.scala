package csw.trombone.assembly.actors

import csw.messages._
import csw.messages.ccs.commands.ControlCommand

trait AssemblyFollowingCommandHandlers extends AssemblyCommandHandlers {
  def onFollowing(controlCommand: ControlCommand): AssemblyCommandState
  def onFollowingCommandComplete(runId: String, result: CommandExecutionResponse): Unit
}
