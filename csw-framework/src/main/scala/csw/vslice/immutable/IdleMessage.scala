package csw.vslice.immutable

import csw.vslice.hcd.models.{AxisRequest, InternalMessages}

sealed trait IdleMessage

object IdleMessage {
  case class IdleAxisRequest(axisRequest: AxisRequest)               extends IdleMessage
  case class IdleInternalMessage(internalMessages: InternalMessages) extends IdleMessage
}
