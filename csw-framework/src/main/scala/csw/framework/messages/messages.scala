package csw.framework.messages

import akka.typed.ActorRef
import csw.framework.immutable.TromboneHcdMessage

sealed trait MotionWorkerMsgs

object MotionWorkerMsgs {
  case class Start(replyTo: ActorRef[MotionWorkerMsgs]) extends MotionWorkerMsgs
  case class End(finalpos: Int)                         extends MotionWorkerMsgs
  case class Tick(current: Int)                         extends MotionWorkerMsgs
  case class MoveUpdate(destination: Int)               extends MotionWorkerMsgs
  case object Cancel                                    extends MotionWorkerMsgs
}

sealed trait AxisState

object AxisState {
  case object AXIS_IDLE   extends AxisState
  case object AXIS_MOVING extends AxisState
  case object AXIS_ERROR  extends AxisState
}

sealed trait AxisRequest
object AxisRequest {
  case object Home                                                extends AxisRequest
  case object Datum                                               extends AxisRequest
  case class Move(position: Int, diagFlag: Boolean = false)       extends AxisRequest
  case object CancelMove                                          extends AxisRequest
  case class GetStatistics(replyTo: ActorRef[TromboneHcdMessage]) extends AxisRequest
  case object PublishAxisUpdate                                   extends AxisRequest
  case class InitialState(replyTo: ActorRef[AxisResponse])        extends AxisRequest
}

sealed trait AxisResponse
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

// Internal
sealed trait InternalMessages
object InternalMessages {
  case object DatumComplete              extends InternalMessages
  case class HomeComplete(position: Int) extends InternalMessages
  case class MoveComplete(position: Int) extends InternalMessages
  case object InitialStatistics          extends InternalMessages
}

sealed trait IdleMessage
object IdleMessage {
  case class IdleAxisRequest(axisRequest: AxisRequest)               extends IdleMessage
  case class IdleInternalMessage(internalMessages: InternalMessages) extends IdleMessage
}
