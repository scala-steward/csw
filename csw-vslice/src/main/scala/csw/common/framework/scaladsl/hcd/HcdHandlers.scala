package csw.common.framework.scaladsl.hcd

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.Component.HcdInfo
import csw.common.framework.models.RunningMsg.DomainMsg
import csw.common.framework.models._
import csw.common.framework.scaladsl.LifecycleHandlers
import csw.param.Parameters.Setup

import scala.reflect.ClassTag

//abstract class HcdHandlers[Msg <: DomainMsg: ClassTag](ctx: ActorContext[HcdMsg], hcdInfo: HcdInfo) {
//  val domainAdapter: ActorRef[Msg]              = ctx.spawnAdapter(DomainHcdMsg.apply)
//  val pubSubRef: ActorRef[PubSub[CurrentState]] = ctx.spawnAnonymous(PubSubActor.behaviour[CurrentState])
//
//  implicit val ec: ExecutionContext = ctx.executionContext
//
//  var isOnline: Boolean = false
//
//  def initialize(): Future[Unit]
//  def onRun(): Unit
//  def onSetup(sc: Setup): Unit
//  def onDomainMsg(msg: Msg): Unit
//
//  def onShutdown(): Unit
//  def onRestart(): Unit
//  def onGoOffline(): Unit
//  def onGoOnline(): Unit
//  def onLifecycleFailureInfo(state: LifecycleState, reason: String): Unit
//
//  def stopChildren(): Unit = {
//    ctx.stop(domainAdapter)
//    ctx.stop(pubSubRef)
//  }
//}

abstract class HcdHandlers[Msg <: DomainMsg: ClassTag](ctx: ActorContext[ComponentMsg], hcdInfo: HcdInfo)
    extends LifecycleHandlers[Msg] {

  def onSetup(sc: Setup): Unit
}
