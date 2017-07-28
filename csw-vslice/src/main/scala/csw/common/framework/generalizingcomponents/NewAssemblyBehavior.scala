package csw.common.framework.generalizingcomponents

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.scaladsl.hcd.HcdHandlers
import csw.common.framework.generalizingcomponents.RunningAssemblyMsgNew.{Oneway, Submit}

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

class NewAssemblyBehavior[Msg <: DomainMsgNew: ClassTag](ctx: ActorContext[AssemblyMsgNew],
                                                         supervisor: ActorRef[ComponentResponseMode],
                                                         hcdHandlers: HcdHandlers[Msg])
    extends Actor.MutableBehavior[RunningAssemblyMsgNew] {

  implicit val ec: ExecutionContext = ctx.executionContext

  override def onMessage(msg: RunningAssemblyMsgNew): Behavior[RunningAssemblyMsgNew] = {
    msg match {
      case _: RunningComponentMsg   =>
      case Submit(command, replyTo) =>
      case Oneway(command, replyTo) =>
    }
    this
  }
}
