package models.alarmstate

import csw.messages.TMTSerializable

sealed trait LatchedState extends TMTSerializable

object LatchedState {
  case object NeedsReset extends LatchedState
  case object Reset      extends LatchedState
  case object NoLatch    extends LatchedState

//  implicit val format: OFormat[LatchedState] = Json.format[LatchedState]
}
