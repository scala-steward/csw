package models.alarmstate

import csw.messages.TMTSerializable
import models.Severity
import play.api.libs.json.{Json, OFormat}

case class AlarmState(
    severity: Severity,
    acknowledgedState: AcknowledgedState,
    latchedState: LatchedState,
    shelvedState: ShelvedState,
    activationState: ActivationState
) extends TMTSerializable

//object AlarmState {
//  implicit val format: OFormat[AlarmState] = Json.format[AlarmState]
//}
