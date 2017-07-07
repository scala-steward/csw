package csw.framework.immutable

import csw.framework.messages.{AxisRequest, InternalMessages}

sealed trait IdleMessage

object IdleMessage {
  case class IdleAxisRequest(axisRequest: AxisRequest)               extends IdleMessage
  case class IdleInternalMessage(internalMessages: InternalMessages) extends IdleMessage
}
