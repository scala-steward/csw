package csw.framework.mutable

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.framework.immutable.AxisConfig
import csw.framework.messages.AxisRequest._
import csw.framework.messages.AxisResponse.{AxisStarted, AxisStatistics, AxisUpdate}
import csw.framework.messages.AxisState.{AXIS_IDLE, AXIS_MOVING}
import csw.framework.messages.InternalMessages.{DatumComplete, HomeComplete, InitialStatistics, MoveComplete}
import csw.framework.messages._

import scala.concurrent.duration.DurationInt

object MutableAxisSimulator {
  def behaviour(axisConfig: AxisConfig, replyTo: Option[ActorRef[AxisResponse]]): Behavior[SimulatorCommand] =
    Actor.mutable(ctx ⇒ new MutableAxisSimulator(ctx)(axisConfig: AxisConfig, replyTo))

  def limitMove(ac: AxisConfig, request: Int): Int = Math.max(Math.min(request, ac.highLimit), ac.lowLimit)

  // Check to see if position is in the "limit" zones
  def isHighLimit(ac: AxisConfig, current: Int): Boolean = current >= ac.highUser

  def isLowLimit(ac: AxisConfig, current: Int): Boolean = current <= ac.lowUser

  def isHomed(ac: AxisConfig, current: Int): Boolean = current == ac.home

  sealed trait State
  object State {
    case object Idle                                    extends State
    case class Home(worker: ActorRef[MotionWorkerMsgs]) extends State
    case class Move(worker: ActorRef[MotionWorkerMsgs]) extends State
  }
}

class MutableAxisSimulator(ctx: ActorContext[SimulatorCommand])(axisConfig: AxisConfig,
                                                                replyTo: Option[ActorRef[AxisResponse]])
    extends Actor.MutableBehavior[SimulatorCommand] {

  import MutableAxisSimulator._

  // Check that the home position is not in a limit area - with this check it is not neceesary to check for limits after homing
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

  var state: State = State.Idle

  override def onMessage(msg: SimulatorCommand): Behavior[SimulatorCommand] = {
    (msg, state) match {
      case (x: AxisRequest, State.Idle)              ⇒ mainReceive(x)
      case (x: MotionWorkerMsgs, State.Home(worker)) ⇒ homeReceive(x, worker)
      case (x: MotionWorkerMsgs, State.Move(worker)) ⇒ moveReceive(x, worker)
      case _                                         ⇒ this
    }

  }

  import MotionWorkerMsgs._

  def mainReceive(msg: AxisRequest): Behavior[SimulatorCommand] = msg match {
    case InitialState(replyToIn) =>
      replyToIn ! getState
      this
    case Datum =>
      axisState = AXIS_MOVING
      update(replyTo, AxisStarted)
      ctx.schedule(1.second, ctx.self, DatumComplete)
      // Stats
      initCount += 1
      moveCount += 1
      this
    case GetStatistics(replyToIn) ⇒
      replyToIn ! AxisStatistics(axisConfig.axisName,
                                 initCount,
                                 moveCount,
                                 homeCount,
                                 limitCount,
                                 successCount,
                                 failureCount,
                                 cancelCount)
      this
    case PublishAxisUpdate =>
      update(replyTo, getState)
      this

    case Home =>
      axisState = AXIS_MOVING
      moveCount += 1
      update(replyTo, AxisStarted)
      println(s"AxisHome: $axisState")

      val beh =
        MutableMotionWorker.behaviour(current, axisConfig.home, delayInMS = 100, ctx.self, diagFlag = false)
      val worker = ctx.spawnAnonymous(beh)

      worker ! Start(ctx.self)
      state = State.Home(worker)
      this
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
      state = State.Move(worker)
      this
    case CancelMove =>
      println("Received Cancel Move while idle :-(")
      cancelCount = cancelCount + 1
      this
  }

  def homeReceive(message: MotionWorkerMsgs, worker: ActorRef[MotionWorkerMsgs]): Behavior[SimulatorCommand] =
    message match {
      case Start(_) =>
        println("Home Start")
        this
      case End(finalpos) =>
        println("Move End")
        state = State.Idle
        //TODO complete message
        this
      case Tick(currentIn) =>
        current = currentIn
        checkLimits()
        update(replyTo, getState)
        this
      case MoveUpdate(destination) =>
        this
      case Cancel =>
        this
    }

  def moveReceive(messsage: MotionWorkerMsgs, worker: ActorRef[MotionWorkerMsgs]): Behavior[SimulatorCommand] =
    messsage match {
      case Start(_) =>
        println("Move Start")
        Actor.same
      case Cancel =>
        println("Cancel MOVE")
        worker ! Cancel
        cancelCount = cancelCount + 1
        state = State.Idle
        this
      case MoveUpdate(targetPosition) =>
        // When this is received, we update the final position while a motion is happening
        worker ! MoveUpdate(targetPosition)
        this
      case Tick(currentIn) =>
        // Set limits - this was a bug - need to do this after every step
        current = currentIn
        checkLimits()
        println("Move Update")
        update(replyTo, getState)
        this
      case End(finalpos) =>
        println("Move End")
        state = State.Idle
        //TODO complete message
        this
    }

  def internalReceive(internalMessages: InternalMessages): Behavior[SimulatorCommand] = internalMessages match {
    case DatumComplete =>
      axisState = AXIS_IDLE
      current += 1
      checkLimits()
      successCount += 1
      update(replyTo, getState)
      this
    case HomeComplete(position) =>
      axisState = AXIS_IDLE
      current = position
      checkLimits()
      if (inHome) homeCount += 1
      successCount += 1
      update(replyTo, getState)
      this
    case MoveComplete(position) =>
      println("Move Complete")
      axisState = AXIS_IDLE
      current = position
      checkLimits()
      if (inHighLimit || inLowLimit) limitCount += 1
      successCount += 1
      update(replyTo, getState)
      this
    case InitialStatistics => this
  }

  private def checkLimits(): Unit = {
    inHighLimit = isHighLimit(axisConfig, current)
    inLowLimit = isLowLimit(axisConfig, current)
    inHome = isHomed(axisConfig, current)
  }

  private def update(replyTo: Option[ActorRef[AxisResponse]], msg: AxisResponse) = replyTo.foreach(_ ! msg)

  private def getState = AxisUpdate(axisConfig.axisName, axisState, current, inLowLimit, inHighLimit, inHome)
}
