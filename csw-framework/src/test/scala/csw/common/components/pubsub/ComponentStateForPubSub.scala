package csw.common.components.pubsub

import csw.messages.commands.CommandName
import csw.messages.params.models.Prefix

object ComponentStateForPubSub {
  val prefix        = Prefix(".prog.cloudcover")
  val invalidPrefix = Prefix("wfos.prog.cloudcover.invalid")

  val moveCmd                   = CommandName("move")
  val initCmd                   = CommandName("init")
  val acceptedCmd               = CommandName("move.accepted")
  val withoutMatcherCmd         = CommandName("move.accept.result")
  val matcherCmd                = CommandName("move.accept.matcher.success.result")
  val matcherFailedCmd          = CommandName("move.accept.matcher.failed.result")
  val matcherTimeoutCmd         = CommandName("move.accept.matcher.success.timeout")
  val immediateCmd              = CommandName("move.immediate")
  val immediateResCmd           = CommandName("move.immediate.result")
  val invalidCmd                = CommandName("move.failure")
  val cancelCmd                 = CommandName("move.cancel")
  val failureAfterValidationCmd = CommandName("move.accept.failure")

  val longRunning   = CommandName("move.longCmd")
  val shortRunning  = CommandName("move.shortCmd")
  val mediumRunning = CommandName("move.mediumCmd")

}
