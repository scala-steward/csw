package csw.vslice.hcd.mutable

import akka.typed.{ActorRef, Behavior, Signal, Terminated}
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.vslice.hcd.messages.PubSub.{Publish, Subscribe, Unsubscribe}
import csw.vslice.hcd.messages.{MsgKey, PubSub}

class PubSubActor[T](key: MsgKey[T])(ctx: ActorContext[PubSub[T]]) extends Actor.MutableBehavior[PubSub[T]] {

  private var subscribers: Set[ActorRef[T]] = Set.empty

  override def onMessage(msg: PubSub[T]): Behavior[PubSub[T]] = {
    msg match {
      case Subscribe(`key`, ref)   => subscribe(ref)
      case Unsubscribe(`key`, ref) => unsubscribe(ref)
      case Publish(data)           => notifySubscribers(data)
      case _                       ⇒
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[PubSub[T]]] = {
    case Terminated(ref) ⇒ unsubscribe(ref.upcast); this
  }

  private def subscribe(actorRef: ActorRef[T]): Unit = {
    if (!subscribers.contains(actorRef)) {
      subscribers += actorRef
      ctx.watch(actorRef)
    }
  }

  private def unsubscribe(actorRef: ActorRef[T]): Unit = {
    subscribers -= actorRef
  }

  protected def notifySubscribers(a: T): Unit = {
    subscribers.foreach(_ ! a)
  }
}
