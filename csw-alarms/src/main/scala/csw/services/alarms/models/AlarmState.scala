package csw.services.alarms.models

import AlarmState._
import akka.util.ByteString
import csw.services.alarms.AlarmModel.SeverityLevel
import csw.services.alarms.models.AlarmEntity.SeverityLevel

object AlarmState {

  // Redis field names
  private[alarms] val acknowledgedStateField = "acknowledgedState"
  private[alarms] val latchedStateField      = "latchedState"
  private[alarms] val latchedSeverityField   = "latchedSeverityLevel"
  private[alarms] val shelvedStateField      = "shelvedState"
  private[alarms] val activationStateField   = "activationState"

  sealed trait AlarmStateItem {
    def name: String

    override def toString: String = name
  }

  sealed trait AcknowledgedState extends AlarmStateItem

  object AcknowledgedState {

    case object NeedsAcknowledge extends AcknowledgedState {
      override val name = "NeedsAcknowledge"
    }

    case object Normal extends AcknowledgedState {
      override val name = "Normal"
    }

    def apply(s: String): AcknowledgedState = s match {
      case NeedsAcknowledge.name => NeedsAcknowledge
      case Normal.name           => Normal
    }
  }

  sealed trait LatchedState extends AlarmStateItem

  object LatchedState {

    object NeedsReset extends LatchedState {
      override val name = "NeedsReset"
    }

    object Normal extends LatchedState {
      override val name = "Normal"
    }

    def apply(s: String): LatchedState = {
      s match {
        case NeedsReset.name => NeedsReset
        case Normal.name     => Normal
      }
    }
  }

  sealed trait ShelvedState extends AlarmStateItem

  object ShelvedState {

    object Shelved extends ShelvedState {
      override val name = "Shelved"
    }

    object Normal extends ShelvedState {
      override val name = "Normal"
    }

    def apply(s: String): ShelvedState = s match {
      case Shelved.name => Shelved
      case Normal.name  => Normal
    }
  }

  sealed trait ActivationState extends AlarmStateItem

  object ActivationState {

    object OutOfService extends ActivationState {
      override val name = "OutOfService"
    }

    object Normal extends ActivationState {
      override val name = "Normal"
    }

    def apply(s: String): ActivationState = s match {
      case OutOfService.name => OutOfService
      case Normal.name       => Normal
    }
  }

  def apply(map: Map[String, String]): AlarmState = {
    AlarmState(
      AcknowledgedState(map(acknowledgedStateField)),
      LatchedState(map(latchedStateField)),
      SeverityLevel(map(latchedSeverityField)).get,
      ShelvedState(map(shelvedStateField)),
      ActivationState(map(activationStateField))
    )
  }
}

case class AlarmState(
    acknowledgedState: AcknowledgedState = AcknowledgedState.Normal,
    latchedState: LatchedState = LatchedState.Normal,
    latchedSeverity: SeverityLevel = SeverityLevel.Disconnected,
    shelvedState: ShelvedState = ShelvedState.Normal,
    activationState: ActivationState = ActivationState.Normal
) {

  def asMap(): Map[String, String] = Map(
    acknowledgedStateField -> acknowledgedState.name,
    latchedStateField      -> latchedState.name,
    latchedSeverityField   -> latchedSeverity.name,
    shelvedStateField      -> shelvedState.name,
    activationStateField   -> activationState.name
  )
}
