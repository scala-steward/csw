package csw.common.framework.generalizingcomponents

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.common.framework.scaladsl.hcd.HcdHandlers
import csw.common.framework.generalizingcomponents.ComponentResponseMode.{Idle, Initialized, Running}
import csw.common.framework.generalizingcomponents.FromComponentLifecycleMessageNew.ShutdownComplete
import csw.common.framework.generalizingcomponents.IdleComponentMsg.{Initialize, Start}
import csw.common.framework.generalizingcomponents.InitialComponentMsg.Run
import csw.common.framework.generalizingcomponents.RunningComponentMsg.{DomainHcdMsg, Lifecycle}
import csw.common.framework.generalizingcomponents.RunningHcdMsgNew.Submit
import csw.common.framework.generalizingcomponents.ToComponentLifecycleMessageNew.{
  GoOffline,
  GoOnline,
  LifecycleFailureInfo,
  Restart,
  Shutdown
}

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

abstract class ComponentBehavior[Msg <: DomainMsgNew: ClassTag](ctx: ActorContext[ComponentMsg],
                                                                supervisor: ActorRef[ComponentResponseMode],
                                                                lifecycleHandlers: LifecycleHandlers[Msg])
    extends Actor.MutableBehavior[ComponentMsg] {

  implicit val ec: ExecutionContext = ctx.executionContext

  var mode: ComponentResponseMode = Idle
  ctx.self ! Initialize

  def runningComponentBehavior(x: RunningComponentMsg): Behavior[ComponentMsg]

  def onMessage(msg: HcdMsgNew): Behavior[ComponentMsg] =
    (mode, msg) match {
      case (Idle, x: IdleComponentMsg)              ⇒ onIdle(x); Actor.same
      case (_: Initialized, x: InitialComponentMsg) ⇒ onInitial(x); Actor.same
      case (_: Running, x: RunningComponentMsg)     ⇒ runningComponentBehavior(x)
      case _                                        ⇒ println(s"current context=$mode does not handle message=$msg"); Actor.same
    }
  this

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
//      lifecycleHandlers.stopChildren()
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
