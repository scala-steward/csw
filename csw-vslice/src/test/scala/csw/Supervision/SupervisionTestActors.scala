package csw.Supervision

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior, PostStop, PreRestart, Signal, SupervisorStrategy, Terminated}

import scala.concurrent.duration.FiniteDuration

sealed trait ToParentMsg
case class Spawn(replyTo: ActorRef[Spawned])    extends ToParentMsg
case object AreYouThere                         extends ToParentMsg
case class StopChild(ref: ActorRef[ToChildMsg]) extends ToParentMsg

sealed trait ToChildMsg
case object Cry  extends ToChildMsg
case object Stop extends ToChildMsg

sealed trait FromActorMsg
case class Spawned(ref: ActorRef[ToChildMsg]) extends FromActorMsg
case class Stopped(message: String)           extends FromActorMsg

object Parent {
  def behavior(): Behavior[ToParentMsg] = Actor.mutable(ctx ⇒ new Parent(ctx))
}
class Parent(ctx: ActorContext[ToParentMsg]) extends MutableBehavior[ToParentMsg] {

  val signalHandler: PartialFunction[Signal, Behavior[ToParentMsg]] = {
    case Terminated(ref) ⇒
      println(s"Terminated $ref")
      this
    case PreRestart ⇒
      println("pre restart")
      this
    case PostStop ⇒
      println("post stop")
      this
  }

  override def onSignal: PartialFunction[Signal, Behavior[ToParentMsg]] = signalHandler

  override def onMessage(msg: ToParentMsg): Behavior[ToParentMsg] = {
    msg match {
      case Spawn(replyTo) =>
        println("Spawning child")
        val child = ctx.spawn(
          Actor
            .supervise(Child.behavior())
            .onFailure[RuntimeException](
              SupervisorStrategy.restartWithLimit(1, FiniteDuration(1, "seconds")).withLoggingEnabled(true)
            ),
          "child"
        )
        ctx.watch(child)
        replyTo ! Spawned(child)
      case AreYouThere      ⇒ println("Parent is alive")
      case StopChild(child) ⇒ child ! Stop
    }
    this
  }
}

object Child {
  def behavior(): Behavior[ToChildMsg] = Actor.mutable(ctx ⇒ new Child(ctx))
}

class Child(ctx: ActorContext[ToChildMsg]) extends MutableBehavior[ToChildMsg] {
  override def onMessage(msg: ToChildMsg): Behavior[ToChildMsg] = {
    msg match {
      case Cry  => println("Child is alive")
      case Stop ⇒ throw new RuntimeException("I am grounded")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[ToChildMsg]] = {
    case PostStop ⇒
      println("Stopping child")
      this
    case PreRestart ⇒
      println("Pre restart of child")
      this
  }
}
