package csw.framework.examples

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.Actor

import scala.concurrent.duration.DurationLong

object MotionWorker {

  sealed trait MotionWorkerMsgs
  case object Start                       extends MotionWorkerMsgs
  case class End(finalpos: Int)           extends MotionWorkerMsgs
  case class Tick(current: Int)           extends MotionWorkerMsgs
  case class MoveUpdate(destination: Int) extends MotionWorkerMsgs
  case object Cancel                      extends MotionWorkerMsgs

  def run(state: State): Behavior[MotionWorkerMsgs] =
    Actor.immutable[MotionWorkerMsgs] { (ctx, msg) ⇒
      msg match {
        case Start =>
          if (state.diagFlag)
            state.diag("Starting", state.start)
          state.replyTo ! Start
          ctx.schedule(state.delayInNanoSeconds.nanos, ctx.self, state.tick)
          Actor.same
        case Tick(current) =>
          state.replyTo ! Tick(current)
          val newState = state
            .copy(current = current)
            .copy(stepCount = state.stepCount + 1)
          newState.log()
          if (!newState.done && !state.cancelFlag)
            ctx.schedule(state.delayInNanoSeconds.nanos, ctx.self, Tick(state.nextPos))
          else
            ctx.self ! End(current)
          run(newState)
        case MoveUpdate(destination) =>
          val newState = state.copy(destination = destination)
          println(s"NEW dest: $destination, numSteps: ${state.numSteps}, stepSize: ${state.stepSize}")
          run(newState)
        case Cancel =>
          if (state.diagFlag) println("Worker received cancel")
          val newState = state.copy(cancelFlag = true)
          run(newState)
        case End(finalpos) =>
          state.replyTo ! msg
          if (state.diagFlag) state.diag("End", finalpos)
          // When the actor has nothing else to do, it should stop
          Actor.stopped
      }
    }

  case class State(
      destination: Int,
      numSteps: Int,
      stepSize: Int,
      stepCount: Int,
      cancelFlag: Boolean,
      current: Int,
      start: Int,
      delayInMS: Int,
      replyTo: ActorRef[MotionWorkerMsgs],
      diagFlag: Boolean
  ) {

    val delayInNanoSeconds: Long = delayInMS * 1000000

    def diag(hint: String, current: Int): Unit =
      println(s"$hint: start=$start, dest=$destination, totalSteps: $numSteps, current=$current")

    def tick = Tick(start + stepSize)

    def calcNumSteps: Int = {
      val diff = Math.abs(start - destination)
      if (diff <= 5) 1
      else if (diff <= 20) 2
      else if (diff <= 500) 5
      else 10
    }

    def calcStepSize: Int = (destination - current) / numSteps

    //def stepNumber(stepCount: Int, numSteps: Int) = numSteps - stepCount

    def calcDistance: Int = Math.abs(current - destination)
    def lastStep: Boolean = calcDistance <= Math.abs(stepSize)
    def nextPos           = if (lastStep) destination else current + stepSize
    def done              = calcNumSteps == 0

    def log() = {
      if (diagFlag)
        println(s"currentIn: $current, distance: $calcNumSteps, stepSize: $stepSize, done: $done, nextPos: $nextPos")

    }
  }

  object State {
    def from(start: Int, destinationIn: Int, delayInMS: Int, replyTo: ActorRef[MotionWorkerMsgs], diagFlag: Boolean) = {
      val state = State(
        destinationIn,
        0,
        0,
        0,
        false,
        start,
        start,
        delayInMS,
        replyTo,
        diagFlag
      )
      state.copy(
        numSteps = state.calcNumSteps,
        stepSize = state.calcStepSize
      )
    }
  }
}
