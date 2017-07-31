package csw.common.framework.generalizingcomponents.api.hcd

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.generalizingcomponents.RunningComponentMsg.DomainComponentMsg
import csw.common.framework.generalizingcomponents.{DomainMsgNew, HcdMsgNew, LifecycleHandlers}
import csw.param.Parameters.Setup

import scala.reflect.ClassTag

abstract class NewHcdHandlers[Msg <: DomainMsgNew: ClassTag](ctx: ActorContext[HcdMsgNew])
    extends LifecycleHandlers[Msg] {
  val domainAdapter: ActorRef[Msg] = ctx.spawnAdapter(DomainComponentMsg.apply)

  def onSetup(sc: Setup): Unit

  def stopChildren(): Unit = {
    ctx.stop(domainAdapter)
  }
}
