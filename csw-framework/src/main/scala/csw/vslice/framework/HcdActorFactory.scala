package csw.vslice.framework

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}

import scala.reflect.ClassTag

abstract class HcdActorFactory[Msg <: DomainMsg: ClassTag] {

  def behavior(supervisor: ActorRef[FromComponentLifecycleMessage]): Behavior[Nothing] =
    Actor.mutable[HcdMsg](ctx ⇒ make(supervisor)(ctx)).narrow

  protected def make(supervisor: ActorRef[FromComponentLifecycleMessage])(ctx: ActorContext[HcdMsg]): HcdActor[Msg]
}
