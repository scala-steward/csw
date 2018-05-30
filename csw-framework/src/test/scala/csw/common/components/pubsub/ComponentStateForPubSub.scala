package csw.common.components.pubsub

import csw.messages.commands.CommandName
import csw.messages.params.models.Prefix

object ComponentStateForPubSub {
  val prefix        = Prefix("test.pubusb.hcd")

  val publishCmd                = CommandName("publish")
}
