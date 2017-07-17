package csw.vslice.ccs

import akka.actor.Cancellable
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import akka.util.Timeout
import csw.param.StateVariable.CurrentState
import csw.vslice.ccs.CommandStatus.CommandResponse
import csw.vslice.ccs.MultiStateMatcherMsgs._
import csw.vslice.framework.PubSub
import csw.vslice.framework.PubSub.{Subscribe, Unsubscribe}

class MultiStateMatcherActor(currentStateReceiver: ActorRef[PubSub[CurrentState]],
                             timeout: Timeout,
                             ctx: ActorContext[MultiStateMatcherMsgs])
    extends MutableBehavior[MultiStateMatcherMsgs] {

  import MultiStateMatcherActor._

  val currentStateAdapter: ActorRef[CurrentState] = ctx.spawnAdapter(StateUpdate)

  var replyTo: ActorRef[CommandResponse] = _
  var context: Context                   = Context.Waiting
  var timer: Cancellable                 = _
  var matchers: List[StateMatcher]       = _

  currentStateReceiver ! Subscribe(currentStateAdapter)

  def onMessage(msg: MultiStateMatcherMsgs): Behavior[MultiStateMatcherMsgs] = {
    (context, msg) match {
      case (Context.Waiting, x: WaitingMsg)     ⇒ onWaiting(x)
      case (Context.Executing, x: ExecutingMsg) ⇒ onExecuting(x)
      case _                                    ⇒ println(s"current context=$context does not handle message=$msg")
    }
    this
  }

  def onWaiting(msg: WaitingMsg): Unit = msg match {
    case StartMatch(replyToIn, matcherIn) =>
      this.replyTo = replyToIn
      this.matchers = matcherIn
      timer = ctx.schedule(timeout.duration, ctx.self, Stop)
      context = Context.Executing
  }

  def onExecuting(msg: ExecutingMsg): Unit = msg match {
    case StateUpdate(current) =>
      val matched = matchers.filter(_.prefix == current.prefixStr).filter(_.check(current))
      if (matched.nonEmpty) {
        val newMatchers = matchers.diff(matched)
        if (newMatchers.isEmpty) {
          timer.cancel()
          currentStateReceiver ! Unsubscribe(currentStateAdapter)
          replyTo ! CommandStatus.Completed
          ctx.stop(currentStateAdapter)
          ctx.stop(ctx.self)
        } else {
          matchers = newMatchers
        }
      }

    case Stop =>
      replyTo ! CommandStatus.Error("Current state matching timed out")
      currentStateReceiver ! Unsubscribe(currentStateAdapter)
      ctx.stop(currentStateAdapter)
      ctx.stop(ctx.self)
  }
}

object MultiStateMatcherActor {

  def make(currentStateReceiver: ActorRef[PubSub[CurrentState]], timeout: Timeout): Behavior[WaitingMsg] =
    Actor.mutable[MultiStateMatcherMsgs](ctx ⇒ new MultiStateMatcherActor(currentStateReceiver, timeout, ctx)).narrow

  sealed trait Context
  object Context {
    case object Waiting   extends Context
    case object Executing extends Context
  }

}
