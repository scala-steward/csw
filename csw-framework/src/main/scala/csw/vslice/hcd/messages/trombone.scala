package csw.vslice.hcd.messages

import akka.typed.ActorRef
import csw.vslice.hcd.models.AxisState
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState

sealed trait SimulatorCommand

sealed trait AxisRequest extends SimulatorCommand
object AxisRequest {
  case object Home                                          extends AxisRequest
  case object Datum                                         extends AxisRequest
  case class Move(position: Int, diagFlag: Boolean = false) extends AxisRequest
  case object CancelMove                                    extends AxisRequest
  case class GetStatistics(replyTo: ActorRef[AxisResponse]) extends AxisRequest
  case object PublishAxisUpdate                             extends AxisRequest
  case class InitialState(replyTo: ActorRef[AxisResponse])  extends AxisRequest
}

// Internal
sealed trait InternalMessages extends SimulatorCommand
object InternalMessages {
  case object DatumComplete              extends InternalMessages
  case class HomeComplete(position: Int) extends InternalMessages
  case class MoveComplete(position: Int) extends InternalMessages
  case object InitialStatistics          extends InternalMessages
}

sealed trait MotionWorkerMsgs extends SimulatorCommand

object MotionWorkerMsgs {
  case class Start(replyTo: ActorRef[MotionWorkerMsgs]) extends MotionWorkerMsgs
  case class End(finalpos: Int)                         extends MotionWorkerMsgs
  case class Tick(current: Int)                         extends MotionWorkerMsgs
  case class MoveUpdate(destination: Int)               extends MotionWorkerMsgs
  case object Cancel                                    extends MotionWorkerMsgs
}

/////////////////////////

sealed trait TromboneMsg

sealed trait Initial extends TromboneMsg
object Initial {
  case object Run              extends Initial
  case object ShutdownComplete extends Initial with Running
}

sealed trait Running extends TromboneMsg
object Running {
  case class Lifecycle(message: ToComponentLifecycleMessage) extends Running
  case class Submit(command: Setup)                          extends Running
  case class Publish(currentState: CurrentState)             extends Running
}

sealed trait TromboneEngineering extends Running
object TromboneEngineering {
  case object GetAxisStats                                     extends TromboneEngineering
  case object GetAxisUpdate                                    extends TromboneEngineering
  case class GetAxisUpdateNow(replyTo: ActorRef[AxisResponse]) extends TromboneEngineering
  case object GetAxisConfig                                    extends TromboneEngineering
}

sealed trait AxisResponse extends Running
object AxisResponse {
  case object AxisStarted                                extends AxisResponse
  case class AxisFinished(newRef: ActorRef[AxisRequest]) extends AxisResponse
  case class AxisUpdate(axisName: String,
                        state: AxisState,
                        current: Int,
                        inLowLimit: Boolean,
                        inHighLimit: Boolean,
                        inHomed: Boolean)
      extends AxisResponse
  case class AxisFailure(reason: String) extends AxisResponse
  case class AxisStatistics(axisName: String,
                            initCount: Int,
                            moveCount: Int,
                            homeCount: Int,
                            limitCount: Int,
                            successCount: Int,
                            failureCount: Int,
                            cancelCount: Int)
      extends AxisResponse {
    override def toString =
      s"name: $axisName, inits: $initCount, moves: $moveCount, homes: $homeCount, limits: $limitCount, success: $successCount, fails: $failureCount, cancels: $cancelCount"
  }
}
