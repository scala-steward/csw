package csw.common.framework.generalizingcomponents.api.assembly

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.generalizingcomponents.internal.assembly.NewAssemblyBehavior
import csw.common.framework.generalizingcomponents.{AssemblyMsgNew, ComponentResponseMode, DomainMsgNew}
import csw.common.framework.models.Component.AssemblyInfo

import scala.reflect.ClassTag

abstract class AssemblyFactory[Msg <: DomainMsgNew: ClassTag] {
  def make(ctx: ActorContext[AssemblyMsgNew], assemblyInfo: AssemblyInfo): NewAssemblyHandlers[Msg]

  def behaviour(assemblyInfo: AssemblyInfo, supervisor: ActorRef[ComponentResponseMode]): Behavior[Nothing] =
    Actor
      .mutable[AssemblyMsgNew](ctx ⇒ new NewAssemblyBehavior[Msg](ctx, supervisor, make(ctx, assemblyInfo)))
      .narrow
}
