package csw.vslice.assembly

import akka.typed.ActorRef
import csw.param.Events.EventTime
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState
import csw.param._
import csw.vslice.assembly.TromboneCommandHandlerMsgs.ExecutingMsgs
import csw.vslice.assembly.TromboneStateActor.{StateWasSet, TromboneState}
import csw.vslice.ccs.CommandStatus.CommandResponse
import csw.vslice.framework.FromComponentLifecycleMessage.Running
import csw.vslice.framework.RunningHcdMsg.Submit

sealed trait FollowCommandMessages
object FollowCommandMessages {
  case class UpdateNssInUse(nssInUse: BooleanParameter)                               extends FollowCommandMessages
  case class UpdateZAandFE(zenithAngle: DoubleParameter, focusError: DoubleParameter) extends FollowCommandMessages
  case class UpdateTromboneHcd(running: Option[ActorRef[Submit]])                     extends FollowCommandMessages
}

////////////////////////

sealed trait FollowActorMessages extends FollowCommandMessages
object FollowActorMessages {
  case class UpdatedEventData(zenithAngle: DoubleParameter, focusError: DoubleParameter, time: EventTime)
      extends FollowActorMessages
  case class SetElevation(elevation: DoubleParameter)     extends FollowActorMessages
  case class SetZenithAngle(zenithAngle: DoubleParameter) extends FollowActorMessages
  case object StopFollowing                               extends FollowActorMessages
}

////////////////////////

sealed trait TrombonePublisherMsg
object TrombonePublisherMsg {
  case class TrombonePublisherMsgE(tromboneState: TromboneState)                 extends TrombonePublisherMsg
  case class AOESWUpdate(naElevation: DoubleParameter, naRange: DoubleParameter) extends TrombonePublisherMsg
  case class EngrUpdate(focusError: DoubleParameter, stagePosition: DoubleParameter, zenithAngle: DoubleParameter)
      extends TrombonePublisherMsg
  case class AxisStateUpdate(axisName: StringParameter,
                             position: IntParameter,
                             state: ChoiceParameter,
                             inLowLimit: BooleanParameter,
                             inHighLimit: BooleanParameter,
                             inHome: BooleanParameter)
      extends TrombonePublisherMsg
  case class AxisStatsUpdate(axisName: StringParameter,
                             initCount: IntParameter,
                             moveCount: IntParameter,
                             homeCount: IntParameter,
                             limitCount: IntParameter,
                             successCount: IntParameter,
                             failCount: IntParameter,
                             cancelCount: IntParameter)
      extends TrombonePublisherMsg
}

///////////////////

sealed trait TromboneControlMsg
object TromboneControlMsg {
  case class UpdateTromboneHcd(running: Option[ActorRef[Submit]]) extends TromboneControlMsg
  case class GoToStagePosition(stagePosition: DoubleParameter)    extends TromboneControlMsg
}

/////////////////////

sealed trait DiagPublisherMessages
object DiagPublisherMessages {
  final case class TimeForAxisStats(periodInseconds: Int)      extends DiagPublisherMessages
  final case object DiagnosticState                            extends DiagPublisherMessages
  final case object OperationsState                            extends DiagPublisherMessages
  final case class CurrentStateE(cs: CurrentState)             extends DiagPublisherMessages
  final case class UpdateTromboneHcd(running: Option[Running]) extends DiagPublisherMessages
}

////////////////////
sealed trait TromboneCommandHandlerMsgs
object TromboneCommandHandlerMsgs {

  sealed trait NotFollowingMsgs extends TromboneCommandHandlerMsgs

  sealed trait FollowingMsgs extends TromboneCommandHandlerMsgs

  sealed trait ExecutingMsgs extends TromboneCommandHandlerMsgs

  sealed trait InitializingMsgs extends TromboneCommandHandlerMsgs

  private[assembly] case object CommandDone                                     extends ExecutingMsgs
  private[assembly] case class CommandStart(replyTo: ActorRef[CommandResponse]) extends ExecutingMsgs
  private[assembly] case class SetStateResponseE(response: StateWasSet)         extends ExecutingMsgs

  case class TromboneStateE(tromboneState: TromboneState) extends InitializingMsgs

  case class Submit(command: Setup, replyTo: ActorRef[CommandResponse])
      extends ExecutingMsgs
      with NotFollowingMsgs
      with FollowingMsgs

}
///////////////////////
sealed trait TromboneCommandMsgs
object TromboneCommandMsgs {
  private[assembly] case class CommandStart(replyTo: ActorRef[CommandResponse]) extends TromboneCommandMsgs
  private[assembly] case object StopCurrentCommand                              extends TromboneCommandMsgs
  private[assembly] case class SetStateResponseE(response: StateWasSet)         extends TromboneCommandMsgs
  private[assembly] case object PoisonPill                                      extends TromboneCommandMsgs
}
