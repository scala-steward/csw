package csw.common.framework.generalizingcomponents.api.hcd

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.generalizingcomponents.internal.hcd.NewHcdBehavior
import csw.common.framework.generalizingcomponents.{ComponentResponseMode, DomainMsgNew, HcdMsgNew}

import scala.reflect.ClassTag

abstract class HcdFactory[Msg <: DomainMsgNew: ClassTag] {

  def make(ctx: ActorContext[HcdMsgNew]): NewHcdHandlers[Msg]

  def behaviour(supervisor: ActorRef[ComponentResponseMode]): Behavior[Nothing] =
    Actor.mutable[HcdMsgNew](ctx ⇒ new NewHcdBehavior[Msg](ctx, supervisor, make(ctx))).narrow
}
