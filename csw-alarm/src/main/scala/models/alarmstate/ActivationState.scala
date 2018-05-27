package models.alarmstate

import csw.messages.TMTSerializable

sealed trait ActivationState extends TMTSerializable

case object ActivationState {
  case object Activated   extends ActivationState
  case object Deactivated extends ActivationState

//  implicit val format: OFormat[ActivationState] = Json.format[ActivationState]
}
