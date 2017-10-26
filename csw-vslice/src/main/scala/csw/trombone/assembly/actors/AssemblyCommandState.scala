package csw.trombone.assembly.actors

import csw.messages.CommandExecutionResponse
import csw.trombone.assembly.commands.AssemblyCommand

case class AssemblyCommandState(
    commandOrResponse: Either[List[AssemblyCommand], CommandExecutionResponse],
    commandExecutionState: CommandExecutionState
)
