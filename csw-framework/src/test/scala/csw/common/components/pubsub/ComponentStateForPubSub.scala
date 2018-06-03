package csw.common.components.pubsub

import csw.messages.commands.CommandName
import csw.messages.params.models.Prefix
import csw.messages.params.states.StateName

object ComponentStateForPubSub {
  val prefix     = Prefix("test.pubsub.hcd")
  val stateName1 = StateName("cs1")
  val stateName2 = StateName("cs2")

  val publishCmd = CommandName("publish")
}
