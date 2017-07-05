package csw.framework.example

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.framework.example.MotionWorker._
import csw.framework.example.TromboneHcdMessage.AxisResponseE

import scala.concurrent.duration.DurationInt

object SingleAxisSimulator {
  sealed trait AxisState
  case object AXIS_IDLE   extends AxisState
  case object AXIS_MOVING extends AxisState
  case object AXIS_ERROR  extends AxisState

  sealed trait AxisRequest
  case object Home                                                extends AxisRequest
  case object Datum                                               extends AxisRequest
  case class Move(position: Int, diagFlag: Boolean = false)       extends AxisRequest
  case object CancelMove                                          extends AxisRequest
  case class GetStatistics(replyTo: ActorRef[TromboneHcdMessage]) extends AxisRequest
  case object PublishAxisUpdate                                   extends AxisRequest

  sealed trait AxisResponse
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

  // Internal
  sealed trait InternalMessages
  case class InitialState(replyTo: ActorRef[AxisResponse]) extends InternalMessages
  case object DatumComplete                                extends InternalMessages
  case class HomeComplete(position: Int)                   extends InternalMessages
  case class MoveComplete(position: Int)                   extends InternalMessages
  case object InitialStatistics                            extends InternalMessages

  sealed trait IdleMessage
  case class IdleAxisRequest(axisRequest: AxisRequest)               extends IdleMessage
  case class IdleInternalMessage(internalMessages: InternalMessages) extends IdleMessage

  case class State(
      axisConfig: AxisConfig,
      replyTo: Option[ActorRef[AxisResponse]],
      current: Int,
      inLowLimit: Boolean = false,
      inHighLimit: Boolean = false,
      inHome: Boolean = false,
      axisState: AxisState = AXIS_IDLE,
      initCount: Int = 0,
      moveCount: Int = 0,
      homeCount: Int = 0,
      limitCount: Int = 0,
      successCount: Int = 0,
      failureCount: Int = 0,
      cancelCount: Int = 0
  ) {
    assert(axisConfig.home > axisConfig.lowUser,
           s"home position must be greater than lowUser value: ${axisConfig.lowUser}")
    assert(axisConfig.home < axisConfig.highUser,
           s"home position must be less than highUser value: ${axisConfig.highUser}")

    def getState = AxisUpdate(axisConfig.axisName, axisState, current, inLowLimit, inHighLimit, inHome)
    def axisStatistics =
      AxisStatistics(axisConfig.axisName,
                     initCount,
                     moveCount,
                     homeCount,
                     limitCount,
                     successCount,
                     failureCount,
                     cancelCount)

    def limitMove(request: Int): Int = Math.max(Math.min(request, axisConfig.highLimit), axisConfig.lowLimit)

    def isHighLimit: Boolean = current >= axisConfig.highUser

    def isLowLimit: Boolean = current <= axisConfig.lowUser

    def isHomed: Boolean = current == axisConfig.home

    def checkLimits(): State = copy(inHighLimit = isHighLimit, inLowLimit = isLowLimit, inHome = isHomed)

    def newHomeCount  = if (inHome) homeCount + 1 else homeCount
    def newLimitCount = if (inHighLimit || inLowLimit) limitCount + 1 else limitCount
  }

  def loop(state: State): Behavior[IdleMessage] =
    Actor.immutable[IdleMessage] { (ctx, msg) ⇒
      msg match {
        case IdleInternalMessage(internalMessages) => handleInternalMessage(state, internalMessages, ctx, loop)
        case IdleAxisRequest(axisRequest)          => handleAxisRequest(state, axisRequest, ctx, loop)
      }
    }

  def handleAxisRequest(state: State,
                        axisRequest: AxisRequest,
                        ctx: ActorContext[IdleMessage],
                        loop: State ⇒ Behavior[IdleMessage]): Behavior[IdleMessage] =
    axisRequest match {
      case Home =>
        val newState = state
          .copy(axisState = AXIS_MOVING)
          .copy(moveCount = state.moveCount + 1)
        println(s"AxisHome: ${newState.axisState}")
        newState.replyTo.foreach(_ ! AxisStarted)
        val workerState = MotionWorker.State
          .from(state.current, state.axisConfig.home, delayInMS = 100, replyTo = None, diagFlag = false)
        val worker  = ctx.spawnAnonymous(MotionWorker.run(workerState))
        val homeRef = ctx.spawnAnonymous(homeReceive(newState))
        worker ! MotionWorker.Start(homeRef)
        Actor.stopped
      case Datum =>
        val newState = state
          .copy(axisState = AXIS_MOVING)
          .copy(initCount = state.initCount + 1)
          .copy(moveCount = state.moveCount + 1)
        newState.replyTo.foreach(_ ! AxisStarted)
        ctx.schedule(1.second, ctx.self, IdleInternalMessage(DatumComplete))
        loop(newState)
      case GetStatistics(replyTo) =>
        replyTo ! AxisResponseE(state.axisStatistics)
        Actor.same
      case Move(position, diagFlag) =>
        val newState = state
          .copy(axisState = AXIS_MOVING)
          .copy(moveCount = state.moveCount + 1)
        println(s"Move: $position")
        val clampedTargetPosition = newState.limitMove(position)
        newState.replyTo.foreach(_ ! AxisStarted)
        val workerState = MotionWorker.State
          .from(state.current,
                clampedTargetPosition,
                delayInMS = state.axisConfig.stepDelayMS,
                replyTo = None,
                diagFlag = diagFlag)
        val worker  = ctx.spawnAnonymous(MotionWorker.run(workerState))
        val moveRef = ctx.spawnAnonymous(moveReceive(newState, worker))
        worker ! MotionWorker.Start(moveRef)
        loop(newState)
      case CancelMove =>
        println("Received Cancel Move while idle :-(")
        val newState = state.copy(cancelCount = state.cancelCount + 1)
        loop(newState)
      case PublishAxisUpdate =>
        state.replyTo.foreach(_ ! state.getState)
        Actor.same
    }

  def handleInternalMessage(state: State,
                            internalMessages: InternalMessages,
                            ctx: ActorContext[IdleMessage],
                            loop: State ⇒ Behavior[IdleMessage]): Behavior[IdleMessage] =
    internalMessages match {
      case InitialState(replyTo) =>
        replyTo ! state.getState
        Actor.same
      case DatumComplete =>
        val newState = state
          .copy(axisState = AXIS_IDLE)
          .copy(current = state.current + 1)
          .copy(successCount = state.successCount + 1)
          .checkLimits()
        newState.replyTo.foreach(_ ! newState.getState)
        loop(newState)
      case HomeComplete(position) =>
        val newState = state
          .copy(axisState = AXIS_IDLE)
          .copy(current = position)
          .checkLimits()
          .copy(homeCount = state.newHomeCount)
          .copy(successCount = state.successCount + 1)
        newState.replyTo.foreach(_ ! state.getState)
        val wrapper = ctx.spawnAdapter { x: AxisRequest ⇒
          IdleAxisRequest(x)
        }
        newState.replyTo.foreach(_ ! AxisFinished(wrapper))
        loop(newState)
      case MoveComplete(position) =>
        val newState = state
          .copy(axisState = AXIS_IDLE)
          .copy(current = position)
          .checkLimits()
          .copy(limitCount = state.newLimitCount)
          .copy(successCount = state.successCount + 1)
        newState.replyTo.foreach(_ ! state.getState)
        val wrapper = ctx.spawnAdapter { x: AxisRequest ⇒
          IdleAxisRequest(x)
        }
        newState.replyTo.foreach(_ ! AxisFinished(wrapper))
        loop(newState)
      case InitialStatistics => Actor.same
    }

  def homeReceive(state: State): Behavior[MotionWorkerMsgs] =
    Actor.immutable[MotionWorkerMsgs] { (ctx, msg) ⇒
      msg match {
        case Start(_) =>
          println("Home Start")
          Actor.same
        case End(finalpos) =>
          val ref = ctx.spawnAnonymous(loop(state))
          ref ! IdleInternalMessage(HomeComplete(finalpos))
          Actor.stopped
        case Tick(current) =>
          val newState = state.copy(current = current).checkLimits()
          state.replyTo.foreach(_ ! state.getState)
          homeReceive(newState)
        case MoveUpdate(destination) =>
          Actor.same
        case Cancel =>
          Actor.same
      }

    }

  def moveReceive(state: State, worker: ActorRef[MotionWorkerMsgs]): Behavior[MotionWorkerMsgs] =
    Actor.immutable[MotionWorkerMsgs] { (ctx, msg) ⇒
      msg match {
        case Start(_) =>
          println("Move Start")
          Actor.same
        case Cancel =>
          println("Cancel MOVE")
          worker ! Cancel
          // Stats
          val newState = state.copy(cancelCount = state.cancelCount + 1)
          moveReceive(newState, worker)
        case MoveUpdate(targetPosition) =>
          // When this is received, we update the final position while a motion is happening
          worker ! MoveUpdate(targetPosition)
          Actor.same
        case Tick(currentIn) =>
          // Set limits - this was a bug - need to do this after every step
          val newState = state.copy(current = currentIn).checkLimits()
          println("Move Update")
          // Send Update to caller
          state.replyTo.foreach(_ ! state.getState)
          moveReceive(newState, worker)
        case End(finalpos) =>
          println("Move End")
          val ref = ctx.spawnAnonymous(loop(state))
          ref ! IdleInternalMessage(MoveComplete(finalpos))
          Actor.stopped
      }
    }

}
