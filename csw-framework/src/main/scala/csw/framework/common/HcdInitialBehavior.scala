package csw.framework.common

import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.framework.immutable.HcdRunningBehavior

object HcdInitialBehavior {
  sealed trait InitialMessage[+T]
  case class Run[T](replyTo: ActorRef[Running[T]]) extends InitialMessage[T]
  case object Shutdown                             extends InitialMessage[Nothing]

  case class Running[T](ref: ActorRef[T])
}

class HcdInitialBehavior[Msg, State](value: HcdRunningBehavior[Msg, State]) {
  import HcdInitialBehavior._
  def run(state: State): Behavior[InitialMessage[Msg]] = Actor.immutable[InitialMessage[Msg]] { (ctx, msg) ⇒
    msg match {
      case Shutdown =>
        println(msg)
        Actor.stopped
      case Run(replyTo) =>
        println(msg)
        replyTo ! Running(ctx.spawnAnonymous(value.run(state)))
        Actor.stopped
    }
  }
}
