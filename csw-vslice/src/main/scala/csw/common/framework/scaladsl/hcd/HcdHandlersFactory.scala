package csw.common.framework.scaladsl.hcd

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.models.Component.HcdInfo
import csw.common.framework.models.{ComponentResponseMode, DomainMsg, HcdMsg}

import scala.reflect.ClassTag

//abstract class HcdHandlersFactory[Msg <: DomainMsg: ClassTag] {
//  def make(ctx: ActorContext[HcdMsg], hcdInfo: HcdInfo): HcdHandlers[Msg]
//
//  def behaviour(hcdInfo: HcdInfo, supervisor: ActorRef[HcdResponseMode]): Behavior[Nothing] =
//    Actor.mutable[HcdMsg](ctx ⇒ new HcdBehavior[Msg](ctx, supervisor, make(ctx, hcdInfo))).narrow
//}

abstract class HcdHandlersFactory[Msg <: DomainMsg: ClassTag] {

  def make(ctx: ActorContext[HcdMsg], hcdInfo: HcdInfo): HcdHandlers[Msg]

  def behaviour(supervisor: ActorRef[ComponentResponseMode], hcdInfo: HcdInfo): Behavior[Nothing] =
    Actor.mutable[HcdMsg](ctx ⇒ new HcdBehavior[Msg](ctx, supervisor, make(ctx, hcdInfo))).narrow
}
