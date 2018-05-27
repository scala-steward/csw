package models.alarmstate

import csw.messages.TMTSerializable

sealed trait AcknowledgedState extends TMTSerializable

object AcknowledgedState {
  case object NeedsAcknowledgement    extends AcknowledgedState
  case object Acknowledged            extends AcknowledgedState
  case object NoAcknowledgementNeeded extends AcknowledgedState

//  implicit val format: OFormat[AcknowledgedState] = Json.format[AcknowledgedState]
}
