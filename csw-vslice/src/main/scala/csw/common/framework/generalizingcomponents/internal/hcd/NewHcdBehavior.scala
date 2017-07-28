package csw.common.framework.generalizingcomponents.internal.hcd

import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.generalizingcomponents.RunningComponentMsg.{DomainComponentMsg, Lifecycle}
import csw.common.framework.generalizingcomponents._
import csw.common.framework.generalizingcomponents.api.hcd.NewHcdHandlers

import scala.reflect.ClassTag

class NewHcdBehavior[Msg <: DomainMsgNew: ClassTag](ctx: ActorContext[HcdMsgNew],
                                                    supervisor: ActorRef[ComponentResponseMode],
                                                    hcdHandlers: NewHcdHandlers[Msg])
    extends ComponentBehavior[Msg](ctx, supervisor, hcdHandlers) {

  override def runningComponentBehavior(x: RunningHcdMsgNew): Behavior[RunningHcdMsgNew] = x match {
    case Lifecycle(message)      =>
    case DomainComponentMsg(msg) =>
  }
}
