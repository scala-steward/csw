package csw.Supervision.typed

import akka.NotUsed
import akka.actor.Scheduler
import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout

import scala.concurrent.duration.DurationLong

object SupervisionApp extends App {
  implicit val timeout = Timeout(2.seconds)

  private val wiring = Actor.deferred[NotUsed] { ctx ⇒
    implicit val scheduler: Scheduler = ctx.system.scheduler
    implicit val ec                   = ctx.executionContext

    val parent = ctx.spawn(Supervisor.behavior(), "parent")

    val child = (parent ? Spawn).map(x ⇒ x.ref)

    child.map(x ⇒ x ! Stop(new RuntimeException("You need to stop")))

    Actor.empty
  }

  ActorSystem("demo", wiring)
}
