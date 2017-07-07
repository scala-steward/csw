package csw.vslice.hcd.immutable

import csw.vslice.hcd.messages.{AxisRequest, InternalMessages}

sealed trait IdleMessage

object IdleMessage {
  case class IdleAxisRequest(axisRequest: AxisRequest)               extends IdleMessage
  case class IdleInternalMessage(internalMessages: InternalMessages) extends IdleMessage
}
