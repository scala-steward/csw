package csw.common.components.pubsub

import csw.messages.commands.CommandName
import csw.messages.params.models.Prefix

object ComponentStateForPubSub {
  val prefix    = Prefix("test.pubsub.hcd")
  val csprefix1 = Prefix("test.pubsub.hcd.cs1")
  val csprefix2 = Prefix("test.pubsub.hcd.cs2")

  val publishCmd = CommandName("publish")
}
