package csw.common.framework.generalizingcomponents

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.scaladsl.hcd.HcdHandlers
import csw.common.framework.generalizingcomponents.RunningHcdMsgNew.Submit

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

class NewHcdBehavior[Msg <: DomainMsgNew: ClassTag](ctx: ActorContext[HcdMsgNew],
                                                    supervisor: ActorRef[ComponentResponseMode],
                                                    hcdHandlers: HcdHandlers[Msg])
    extends Actor.MutableBehavior[RunningHcdMsgNew] {

  implicit val ec: ExecutionContext = ctx.executionContext

  override def onMessage(msg: RunningHcdMsgNew): Behavior[RunningHcdMsgNew] = {
    msg match {
      case _: RunningComponentMsg =>
      case Submit(command)        =>
    }
    this
  }
}
