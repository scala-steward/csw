package csw.common.framework.generalizingcomponents.internal.hcd

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.generalizingcomponents.HcdMsgNew.Submit
import csw.common.framework.generalizingcomponents._
import csw.common.framework.generalizingcomponents.api.hcd.NewHcdHandlers

import scala.reflect.ClassTag

class NewHcdBehavior[Msg <: DomainMsgNew: ClassTag](ctx: ActorContext[HcdMsgNew],
                                                    supervisor: ActorRef[ComponentResponseMode],
                                                    hcdHandlers: NewHcdHandlers[Msg])
    extends ComponentBehavior[Msg, HcdMsgNew](ctx, supervisor, hcdHandlers) {

  override def onRunningCompCommandMsg(x: HcdMsgNew with RunMsg): Unit = x match {
    case Submit(a) ⇒ hcdHandlers.onSetup(a)
  }
}
