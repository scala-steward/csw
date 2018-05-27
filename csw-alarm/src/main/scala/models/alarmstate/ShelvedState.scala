package models.alarmstate

import csw.messages.TMTSerializable

sealed trait ShelvedState extends TMTSerializable

object ShelvedState {
  case object Shelved extends ShelvedState
  case object Normal  extends ShelvedState

//  implicit val format: OFormat[ShelvedState] = Json.format[ShelvedState]
}
