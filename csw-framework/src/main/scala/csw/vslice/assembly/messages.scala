package csw.vslice.assembly

import akka.typed.ActorRef
import csw.param.Events.EventTime
import csw.param.Parameters.{ControlCommand, Setup}
import csw.param.StateVariable.CurrentState
import csw.param._
import csw.vslice.assembly.TromboneStateActor.{StateWasSet, TromboneState}
import csw.vslice.ccs.CommandStatus.CommandResponse
import csw.vslice.framework.FromComponentLifecycleMessage.Running
import csw.vslice.framework.RunningHcdMsg.Submit
import csw.vslice.framework._

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
  case class TromboneStateE(tromboneState: TromboneState) extends TromboneCommandHandlerMsgs

  sealed trait NotFollowingMsgs extends TromboneCommandHandlerMsgs
  sealed trait FollowingMsgs    extends TromboneCommandHandlerMsgs
  sealed trait ExecutingMsgs    extends TromboneCommandHandlerMsgs

  case class Submit(command: Setup, replyTo: ActorRef[CommandResponse])
      extends ExecutingMsgs
      with NotFollowingMsgs
      with FollowingMsgs

  private[assembly] case class CommandStart(replyTo: ActorRef[CommandResponse]) extends ExecutingMsgs
}

///////////////////////
sealed trait TromboneCommandMsgs
object TromboneCommandMsgs {
  private[assembly] case class CommandStart(replyTo: ActorRef[CommandResponse]) extends TromboneCommandMsgs
  private[assembly] case object StopCurrentCommand                              extends TromboneCommandMsgs
  private[assembly] case class SetStateResponseE(response: StateWasSet)         extends TromboneCommandMsgs
}
///////////////////////////

sealed trait ToComponentLifecycleMessage

object ToComponentLifecycleMessage {
  case object DoShutdown                                                 extends ToComponentLifecycleMessage
  case object DoRestart                                                  extends ToComponentLifecycleMessage
  case object Running                                                    extends ToComponentLifecycleMessage
  case object RunningOffline                                             extends ToComponentLifecycleMessage
  case class LifecycleFailureInfo(state: LifecycleState, reason: String) extends ToComponentLifecycleMessage
}

////////////////////

sealed trait FromComponentLifecycleMessage

object FromComponentLifecycleMessage {
  case class Initialized(assemblyRef: ActorRef[InitialAssemblyMsg]) extends FromComponentLifecycleMessage
  case class Running(assemblyRef: ActorRef[RunningAssemblyMsg])
  case class InitializeFailure(reason: String) extends FromComponentLifecycleMessage
  case object ShutdownComplete                 extends FromComponentLifecycleMessage
  case class ShutdownFailure(reason: String)   extends FromComponentLifecycleMessage
  case object HaltComponent                    extends FromComponentLifecycleMessage
}

//////////////////////////
sealed trait AssemblyMsg
sealed trait InitialAssemblyMsg extends AssemblyMsg
object InitialAssemblyMsg {
  case class Run(replyTo: ActorRef[Running]) extends InitialAssemblyMsg
}

sealed trait RunningAssemblyMsg extends AssemblyMsg
object RunningAssemblyMsg {
  case class Lifecycle(message: ToComponentLifecycleMessage)                     extends RunningAssemblyMsg
  case class Submit(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends RunningAssemblyMsg
  case class Oneway(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends RunningAssemblyMsg
  case class DiagMsgs(mode: DiagPublisherMessages)                               extends RunningAssemblyMsg
}

trait DomainMsg
