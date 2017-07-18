package csw.trombone.framework

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}

import scala.reflect.ClassTag

abstract class HcdActorFactory[Msg <: DomainMsg: ClassTag] {

  def behavior(supervisor: ActorRef[HcdComponentLifecycleMessage]): Behavior[Nothing] =
    Actor.mutable[HcdMsg](ctx ⇒ make(ctx, supervisor)).narrow

  protected def make(ctx: ActorContext[HcdMsg], supervisor: ActorRef[HcdComponentLifecycleMessage]): HcdActor[Msg]
}
