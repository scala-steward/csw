package csw.vslice.framework

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.param.StateVariable.CurrentState

import scala.reflect.ClassTag

abstract class HcdActorFactory[Msg <: DomainMsg: ClassTag] extends PubsSubMsgFactory[CurrentState] {

  def behavior(supervisor: ActorRef[FromComponentLifecycleMessage]): Behavior[Nothing] = {
    Actor.mutable[HcdMsg](ctx ⇒ make(supervisor, ctx.spawnAnonymous(PubSubActor.behaviour(this)))(ctx)).narrow
  }

  protected def make(supervisor: ActorRef[FromComponentLifecycleMessage], pubSubRef: ActorRef[PubSub[CurrentState]])(
      ctx: ActorContext[HcdMsg]
  ): HcdActor[Msg]
}
