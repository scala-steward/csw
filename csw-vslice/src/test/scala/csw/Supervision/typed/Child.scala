package csw.Supervision.typed

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{Behavior, PostStop, PreRestart, Signal}

object Child {
  def behavior(): Behavior[ToChildMsg] = Actor.mutable(ctx ⇒ new Child(ctx))
}

class Child(ctx: ActorContext[ToChildMsg]) extends MutableBehavior[ToChildMsg] {
  var state: Int = 0
  override def onMessage(msg: ToChildMsg): Behavior[ToChildMsg] = {
    msg match {
      case UpdateState(x) =>
        state = x
      case GetState(replyTo) =>
        replyTo ! CurrentState(state)
        println(s"Current state is $state")
      case Stop(ex) ⇒
        println("child throwing exception")
        throw ex
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
