package csw.framework.examples

import akka.typed.Behavior
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.framework.lifecycle.ToComponentLifecycleMessage

object HcdRunningBehavior {
  sealed trait RunningMessage[+T]
  case class DomainSpecific[T](msg: T)                       extends RunningMessage[T]
  case class Lifecycle(message: ToComponentLifecycleMessage) extends RunningMessage[Nothing]
}

abstract class HcdRunningBehavior[Msg, State] {
  import HcdRunningBehavior._

  def run(state: State): Behavior[RunningMessage[Msg]] =
    Actor.immutable { (ctx, msg) ⇒
      msg match {
        case DomainSpecific(messages) => domainSpecific(state, ctx, messages, run)
        case Lifecycle(message)       => lifecycle(state, ctx, message, run)
      }
    }

  protected def domainSpecific(state: State,
                               ctx: ActorContext[RunningMessage[Msg]],
                               msg: Msg,
                               loop: State ⇒ Behavior[RunningMessage[Msg]]): Behavior[RunningMessage[Msg]]

  protected def lifecycle(state: State,
                          ctx: ActorContext[RunningMessage[Msg]],
                          msg: ToComponentLifecycleMessage,
                          loop: State ⇒ Behavior[RunningMessage[Msg]]): Behavior[RunningMessage[Msg]]
}
