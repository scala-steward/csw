package csw.vslice.hcd.mutable

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.vslice.hcd.messages.AxisRequest._
import csw.vslice.hcd.messages.AxisResponse.{AxisStarted, AxisStatistics, AxisUpdate}
import csw.vslice.hcd.messages.InternalMessages.{DatumComplete, HomeComplete, InitialStatistics, MoveComplete}
import csw.vslice.hcd.messages._
import csw.vslice.hcd.models.AxisState.{AXIS_IDLE, AXIS_MOVING}
import csw.vslice.hcd.models.{AxisConfig, AxisState}

import scala.concurrent.duration.DurationInt

object MutableAxisSimulator {
  def behaviour(axisConfig: AxisConfig, replyTo: Option[ActorRef[AxisResponse]]): Behavior[AxisRequest] =
    Actor.mutable[SimulatorCommand](ctx ⇒ new MutableAxisSimulator(ctx)(axisConfig: AxisConfig, replyTo)).narrow

  def limitMove(ac: AxisConfig, request: Int): Int = Math.max(Math.min(request, ac.highLimit), ac.lowLimit)

  // Check to see if position is in the "limit" zones
  def isHighLimit(ac: AxisConfig, current: Int): Boolean = current >= ac.highUser

  def isLowLimit(ac: AxisConfig, current: Int): Boolean = current <= ac.lowUser

  def isHomed(ac: AxisConfig, current: Int): Boolean = current == ac.home

  sealed trait Context
  object Context {
    case object Idle                                    extends Context
    case class Home(worker: ActorRef[MotionWorkerMsgs]) extends Context
    case class Move(worker: ActorRef[MotionWorkerMsgs]) extends Context
  }
}

class MutableAxisSimulator(ctx: ActorContext[SimulatorCommand])(axisConfig: AxisConfig,
                                                                replyTo: Option[ActorRef[AxisResponse]])
    extends Actor.MutableBehavior[SimulatorCommand] {

  import MutableAxisSimulator._

  // Check that the home position is not in a limit area - with  check it is not neceesary to check for limits after homing
  assert(axisConfig.home > axisConfig.lowUser,
         s"home position must be greater than lowUser value: ${axisConfig.lowUser}")
  assert(axisConfig.home < axisConfig.highUser,
         s"home position must be less than highUser value: ${axisConfig.highUser}")

  private var current              = axisConfig.startPosition
  private var inLowLimit           = false
  private var inHighLimit          = false
  private var inHome               = false
  private var axisState: AxisState = AXIS_IDLE

  private var initCount    = 0
  private var moveCount    = 0
  private var homeCount    = 0
  private var limitCount   = 0
  private var successCount = 0
  private val failureCount = 0
  private var cancelCount  = 0

  var context: Context = Context.Idle

  override def onMessage(msg: SimulatorCommand): Behavior[SimulatorCommand] = {
    (msg, context) match {
      case (x: AxisRequest, Context.Idle)              ⇒ mainReceive(x)
      case (x: MotionWorkerMsgs, Context.Home(worker)) ⇒ homeReceive(x, worker)
      case (x: MotionWorkerMsgs, Context.Move(worker)) ⇒ moveReceive(x, worker)
      case _                                           ⇒ println(s"current context=$context does not handle message=$msg")
    }
    this
  }

  import MotionWorkerMsgs._

  def mainReceive(msg: AxisRequest): Unit = msg match {
    case InitialState(replyToIn) =>
      replyToIn ! getState

    case Datum =>
      axisState = AXIS_MOVING
      update(replyTo, AxisStarted)
      ctx.schedule(1.second, ctx.self, DatumComplete)
      // Stats
      initCount += 1
      moveCount += 1

    case GetStatistics(replyToIn) ⇒
      replyToIn ! AxisStatistics(axisConfig.axisName,
                                 initCount,
                                 moveCount,
                                 homeCount,
                                 limitCount,
                                 successCount,
                                 failureCount,
                                 cancelCount)

    case PublishAxisUpdate =>
      update(replyTo, getState)

    case Home =>
      axisState = AXIS_MOVING
      moveCount += 1
      update(replyTo, AxisStarted)
      println(s"AxisHome: $axisState")

      val beh =
        MutableMotionWorker.behaviour(current, axisConfig.home, delayInMS = 100, ctx.self, diagFlag = false)
      val worker = ctx.spawnAnonymous(beh)

      worker ! Start(ctx.self)
      context = Context.Home(worker)

    case Move(position, diagFlag) =>
      axisState = AXIS_MOVING
      moveCount = moveCount + 1
      update(replyTo, AxisStarted)
      println(s"Move: $position")

      val beh = MutableMotionWorker.behaviour(current,
                                              limitMove(axisConfig, position),
                                              delayInMS = axisConfig.stepDelayMS,
                                              ctx.self,
                                              diagFlag)
      val worker = ctx.spawn(beh, s"moveWorker-${System.currentTimeMillis}")

      worker ! Start(ctx.self)
      context = Context.Move(worker)

    case CancelMove =>
      println("Received Cancel Move while idle :-(")
      cancelCount = cancelCount + 1

  }

  def homeReceive(message: MotionWorkerMsgs, worker: ActorRef[MotionWorkerMsgs]): Unit =
    message match {
      case Start(_) =>
        println("Home Start")

      case End(finalpos) =>
        println("Move End")
        context = Context.Idle
        ctx.self ! HomeComplete(finalpos)

      case Tick(currentIn) =>
        current = currentIn
        checkLimits()
        update(replyTo, getState)

      case MoveUpdate(destination) =>
      case Cancel                  =>
    }

  def moveReceive(messsage: MotionWorkerMsgs, worker: ActorRef[MotionWorkerMsgs]): Unit =
    messsage match {
      case Start(_) =>
        println("Move Start")

      case Cancel =>
        println("Cancel MOVE")
        worker ! Cancel
        cancelCount = cancelCount + 1
        context = Context.Idle

      case MoveUpdate(targetPosition) =>
        // When  is received, we update the final position while a motion is happening
        worker ! MoveUpdate(targetPosition)

      case Tick(currentIn) =>
        // Set limits -  was a bug - need to do  after every step
        current = currentIn
        checkLimits()
        println("Move Update")
        update(replyTo, getState)

      case End(finalpos) =>
        println("Move End")
        context = Context.Idle
        ctx.self ! MoveComplete(finalpos)

    }

  def internalReceive(internalMessages: InternalMessages): Unit = internalMessages match {
    case DatumComplete =>
      axisState = AXIS_IDLE
      current += 1
      checkLimits()
      successCount += 1
      update(replyTo, getState)

    case HomeComplete(position) =>
      axisState = AXIS_IDLE
      current = position
      checkLimits()
      if (inHome) homeCount += 1
      successCount += 1
      update(replyTo, getState)

    case MoveComplete(position) =>
      println("Move Complete")
      axisState = AXIS_IDLE
      current = position
      checkLimits()
      if (inHighLimit || inLowLimit) limitCount += 1
      successCount += 1
      update(replyTo, getState)

    case InitialStatistics =>
  }

  private def checkLimits(): Unit = {
    inHighLimit = isHighLimit(axisConfig, current)
    inLowLimit = isLowLimit(axisConfig, current)
    inHome = isHomed(axisConfig, current)
  }

  private def update(replyTo: Option[ActorRef[AxisResponse]], msg: AxisResponse) = replyTo.foreach(_ ! msg)

  private def getState = AxisUpdate(axisConfig.axisName, axisState, current, inLowLimit, inHighLimit, inHome)
}
