package csw.common.framework.internal.extensions

import akka.actor.{ActorSystem, CoordinatedShutdown, Scheduler}
import akka.typed.scaladsl.Actor
import akka.typed.scaladsl.AskPattern.Askable
import akka.typed.scaladsl.adapter.{TypedActorSystemOps, UntypedActorSystemOps}
import akka.typed.{ActorRef, Behavior, Props, Terminated}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

object RichSystemExtension {
  implicit val timeout: Timeout = Timeout(2.seconds)

  sealed trait GuardianBehaviorMsg
  case class CreateActor[T](behavior: Behavior[T], name: String, props: Props)(val replyTo: ActorRef[ActorRef[T]])
      extends GuardianBehaviorMsg

  def behavior: Behavior[GuardianBehaviorMsg] =
    Actor.immutable[GuardianBehaviorMsg] {
      case (ctx, msg) ⇒
        msg match {
          case create: CreateActor[t] => {
            val componentRef = ctx.spawn(create.behavior, create.name, create.props)
            ctx.watch(componentRef)
            create.replyTo ! componentRef
          }
        }
        Actor.same
    } onSignal {
      case (ctx, Terminated(ref)) ⇒
        CoordinatedShutdown(ctx.system.toUntyped).run()
        Actor.stopped
    }

  class RichSystem(val system: ActorSystem) {
    implicit def sched: Scheduler = system.scheduler
    private val rootActor         = system.spawn(behavior, "system")
    def spawnTyped[T](behavior: Behavior[T], name: String, props: Props = Props.empty): Future[ActorRef[T]] = {
      rootActor ? CreateActor(behavior, name, props)
    }
  }
}
