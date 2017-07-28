package csw.common.framework.generalizingcomponents.api.assembly

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.ccs.Validation.Validation
import csw.common.framework.generalizingcomponents.{AssemblyMsgNew, LifecycleHandlers}
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models._
import csw.param.Parameters.{Observe, Setup}

import scala.reflect.ClassTag

abstract class NewAssemblyHandlers[Msg <: DomainMsg: ClassTag](ctx: ActorContext[AssemblyMsgNew], info: AssemblyInfo)
    extends LifecycleHandlers[Msg](ctx) {
  val runningHcd: Option[HcdResponseMode.Running] = None

  implicit val scheduler: Scheduler = ctx.system.scheduler

  def setup(s: Setup, commandOriginator: Option[ActorRef[CommandResponse]]): Validation
  def observe(o: Observe, replyTo: Option[ActorRef[CommandResponse]]): Validation
}
