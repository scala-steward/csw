package csw.framework.immutable

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.Actor
import csw.framework.messages.MotionWorkerMsgs

import scala.concurrent.duration.DurationLong

object MotionWorker {

  import csw.framework.messages.MotionWorkerMsgs._

  def run(state: State): Behavior[MotionWorkerMsgs] =
    Actor.immutable[MotionWorkerMsgs] { (ctx, msg) ⇒
      msg match {
        case Start(replyTo) =>
          val newState = state.copy(replyTo = Some(replyTo))
          if (newState.diagFlag)
            newState.diag("Starting", newState.start)
          newState.replyTo.foreach(_ ! Start(replyTo))
          ctx.schedule(newState.delayInNanoSeconds.nanos, ctx.self, newState.tick)
          run(newState)
        case Tick(current) =>
          state.replyTo.foreach(_ ! Tick(current))
          val newState = state
            .copy(current = current)
            .copy(stepCount = state.stepCount + 1)
          newState.log()
          if (!newState.done && !newState.cancelFlag)
            ctx.schedule(newState.delayInNanoSeconds.nanos, ctx.self, Tick(newState.nextPos))
          else
            ctx.self ! End(current)
          run(newState)
        case MoveUpdate(destination) =>
          val newState = state.copy(destination = destination)
          println(s"NEW dest: $destination, numSteps: ${newState.numSteps}, stepSize: ${newState.stepSize}")
          run(newState)
        case Cancel =>
          if (state.diagFlag) println("Worker received cancel")
          val newState = state.copy(cancelFlag = true)
          run(newState)
        case End(finalpos) =>
          state.replyTo.foreach(_ ! msg)
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
      replyTo: Option[ActorRef[MotionWorkerMsgs]],
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
    def from(start: Int,
             destinationIn: Int,
             delayInMS: Int,
             replyTo: Option[ActorRef[MotionWorkerMsgs]],
             diagFlag: Boolean) = {
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
