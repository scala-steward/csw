package csw.vslice.framework

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior, Signal, Terminated}
import csw.vslice.framework.PubSub.{Publish, Subscribe, Unsubscribe}

object PubSubActor {
  def behaviour[T](pubsSubMsgFactory: PubsSubMsgFactory[T]): Behavior[PubSub[T]] =
    Actor.mutable(ctx ⇒ new PubSubActor[T](pubsSubMsgFactory)(ctx))
}

class PubSubActor[T](key: PubsSubMsgFactory[T])(ctx: ActorContext[PubSub[T]])
    extends Actor.MutableBehavior[PubSub[T]] {

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
