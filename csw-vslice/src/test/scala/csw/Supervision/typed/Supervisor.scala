package csw.Supervision.typed

import akka.typed.{ActorRef, Behavior, PostStop, PreRestart, Signal, SupervisorStrategy, Terminated}
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}

import scala.concurrent.duration.FiniteDuration

class Supervisor(ctx: ActorContext[ToParentMsg]) extends MutableBehavior[ToParentMsg] {

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
    var child: ActorRef[ToChildMsg] = null
    msg match {
      case Spawn(replyTo) =>
        println("Spawning child")
        child = ctx.spawn(
          Actor
            .supervise(Child.behavior())
            .onFailure[RuntimeException](
              SupervisorStrategy.restartWithLimit(1, FiniteDuration(1, "seconds")).withLoggingEnabled(true)
            ),
          "child"
        )
        ctx.watch(child)
        replyTo ! Spawned(child)
      case AreYouThere ⇒
        println("Parent is alive")
    }
    this
  }
}

object Supervisor {
  def behavior(): Behavior[ToParentMsg] = Actor.mutable(ctx ⇒ new Supervisor(ctx))
}
