package csw.common.framework.generalizingcomponents.internal.assembly

import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.generalizingcomponents.RunningAssemblyMsgNew.{Oneway, Submit}
import csw.common.framework.generalizingcomponents._
import csw.common.framework.scaladsl.assembly.AssemblyHandlers

import scala.reflect.ClassTag

class NewAssemblyBehavior[Msg <: DomainMsgNew: ClassTag](ctx: ActorContext[AssemblyMsgNew],
                                                         supervisor: ActorRef[ComponentResponseMode],
                                                         assemblyHandlers: AssemblyHandlers[Msg])
    extends ComponentBehavior[RunningAssemblyMsgNew](ctx, supervisor, assemblyHandlers) {

  override def runningComponentBehavior(x: RunningAssemblyMsgNew): Behavior[RunningAssemblyMsgNew] = x match {
    case _: RunningComponentMsg   =>
    case Submit(command, replyTo) =>
    case Oneway(command, replyTo) =>
  }
}
