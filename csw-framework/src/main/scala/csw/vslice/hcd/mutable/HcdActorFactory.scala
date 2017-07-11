package csw.vslice.hcd.mutable

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.param.StateVariable.CurrentState
import csw.vslice.hcd.messages._

import scala.reflect.ClassTag

abstract class HcdActorFactory[Msg <: DomainMsg: ClassTag] extends PubsSubMsgFactory[CurrentState] {
  def behavior(supervisor: ActorRef[FromComponentLifecycleMessage]): Behavior[Nothing] = {
    val pubSubB: Behavior[PubSub[CurrentState]] = Actor.mutable(
      ctx ⇒ new PubSubActor[CurrentState](this)(ctx)
    )
    Actor.mutable[HcdMsg](ctx ⇒ make(supervisor, ctx.spawnAnonymous(pubSubB))(ctx)).narrow
  }

  protected def make(supervisor: ActorRef[FromComponentLifecycleMessage], pubSubRef: ActorRef[PubSub[CurrentState]])(
      ctx: ActorContext[HcdMsg]
  ): HcdActor[Msg]
}
