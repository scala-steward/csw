package csw.common.framework.generalizingcomponents

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.generalizingcomponents.ComponentResponseMode.{Idle, Initialized, Running}
import csw.common.framework.generalizingcomponents.FromComponentLifecycleMessageNew.ShutdownComplete
import csw.common.framework.generalizingcomponents.IdleComponentMsg.{Initialize, Start}
import csw.common.framework.generalizingcomponents.InitialComponentMsg.Run
import csw.common.framework.generalizingcomponents.RunningComponentMsg.{DomainComponentMsg, Lifecycle}
import csw.common.framework.generalizingcomponents.ToComponentLifecycleMessageNew.{GoOffline, GoOnline, LifecycleFailureInfo, Restart, Shutdown}

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

abstract class ComponentBehavior[Msg <: DomainMsgNew : ClassTag, CompMsg >: ComponentMsg](ctx: ActorContext[CompMsg],
                                                                                          supervisor: ActorRef[ComponentResponseMode],
                                                                                          lifecycleHandlers: LifecycleHandlers[Msg, CompMsg])
  extends Actor.MutableBehavior[CompMsg] {

  implicit val ec: ExecutionContext = ctx.executionContext

  var mode: ComponentResponseMode = Idle
  ctx.self ! Initialize

  def onRun(x: RunningComponentMsg): Unit = x match {
    case Lifecycle(message) => onLifecycle(message)
    case DomainComponentMsg(message: Msg) => lifecycleHandlers.onDomainMsg(message)
  }

  def onRunningCompCommandMsg(x: CompMsg with RunMsg): Unit

  def onMessage(msg: CompMsg): Behavior[CompMsg] = {
    (mode, msg) match {
      case (Idle, x: IdleComponentMsg) ⇒ onIdle(x)
      case (_: Initialized, x: InitialComponentMsg) ⇒ onInitial(x)
      case (_: Running, x: RunningComponentMsg) ⇒ onRun(x)
      case (_: Running, x: CompMsg with RunMsg) ⇒ onRunningCompCommandMsg(x)
      case _ ⇒ println(s"current context=$mode does not handle message=$msg")
    }
    this
  }

  def initialization(): Future[Unit] = async {
    await(lifecycleHandlers.initialize())
    mode = Initialized(ctx.self)
  }

  private def onIdle(x: IdleComponentMsg): Unit = x match {
    case Initialize =>
      async {
        await(initialization())
        supervisor ! mode
      }
    case Start ⇒
      async {
        await(initialization())
        ctx.self ! Run
      }
  }

  private def onInitial(x: InitialComponentMsg): Unit = x match {
    case Run =>
      lifecycleHandlers.onRun()
      val running = Running(ctx.self.upcast)
      mode = running
      lifecycleHandlers.isOnline = true
      supervisor ! running
  }

  private def onLifecycle(message: ToComponentLifecycleMessageNew): Unit = message match {
    case Shutdown =>
      lifecycleHandlers.onShutdown()
      supervisor ! ShutdownComplete
    case Restart =>
      lifecycleHandlers.onRestart()
      mode = Idle
      ctx.self ! Start
    case GoOnline =>
      if (!lifecycleHandlers.isOnline) {
        lifecycleHandlers.onGoOnline()
        lifecycleHandlers.isOnline = true
      }
    case GoOffline =>
      if (lifecycleHandlers.isOnline) {
        lifecycleHandlers.onGoOffline()
        lifecycleHandlers.isOnline = false
      }
    case LifecycleFailureInfo(state, reason) => lifecycleHandlers.onLifecycleFailureInfo(state, reason)
  }
}
