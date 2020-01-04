package csw.common.components.command

import csw.params.commands.CommandName
import csw.params.core.generics.{GChoiceKey, KeyType}
import csw.params.core.generics.KeyType.ChoiceKey
import csw.params.core.models.{Choice, Choices}
import csw.prefix.models.Prefix

// Fixme: This file may be merged with `CommandComponentState` or may be renamed
object ComponentStateForCommand {
  val encoder = KeyType.IntKey.make("encoder")

  val prefix        = Prefix("wfos.blue.filter")
  val invalidPrefix = Prefix("wfos.blue.filter.invalid")

  val moveCmd                   = CommandName("move")
  val initCmd                   = CommandName("init")
  val acceptedCmd               = CommandName("move.accepted")
  val longRunningCmd            = CommandName("move.accept.result")
  val onewayCmd                 = CommandName("move.oneway.accept")
  val matcherCmd                = CommandName("move.accept.matcher.success.result")
  val matcherFailedCmd          = CommandName("move.accept.matcher.failed.result")
  val matcherTimeoutCmd         = CommandName("move.accept.matcher.success.timeout")
  val assemCurrentStateCmd      = CommandName("assem.send.current.state")
  val hcdCurrentStateCmd        = CommandName("hcd.send.current.state")
  val crmParentCommandCmd       = CommandName("hcd.parent.crm")
  val crmAddOrUpdateCmd         = CommandName("hcd.update.crm")
  val immediateCmd              = CommandName("move.immediate")
  val immediateResCmd           = CommandName("move.immediate.result")
  val invalidCmd                = CommandName("move.failure")
  val cancelCmd                 = CommandName("move.cancel")
  val failureAfterValidationCmd = CommandName("move.accept.failure")

  val longRunning   = CommandName("move.longCmd")
  val shortRunning  = CommandName("move.shortCmd")
  val mediumRunning = CommandName("move.mediumCmd")

  val longRunningCmdCompleted  = Choice("Long Running Cmd Completed")
  val longRunningCurrentStatus = Choice("Long Running Cmd Completed")
  val shortCmdCompleted        = Choice("Short Running Sub Cmd Completed")
  val mediumCmdCompleted       = Choice("Medium Running Sub Cmd Completed")
  val longCmdCompleted         = Choice("Long Running Sub Cmd Completed")

  val choices: Choices = Choices.fromChoices(
    shortCmdCompleted,
    mediumCmdCompleted,
    longCmdCompleted,
    longRunningCmdCompleted,
    longRunningCurrentStatus
  )
  val choiceKey: GChoiceKey = ChoiceKey.make("choiceKey", choices)
}
