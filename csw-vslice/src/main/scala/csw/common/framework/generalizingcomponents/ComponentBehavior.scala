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

class ComponentBehavior[Msg <: DomainMsgNew: ClassTag](ctx: ActorContext[HcdMsgNew],
                                                       runningComponentBehavior: Behavior[RunningComponentMsg],
                                                       supervisor: ActorRef[ComponentResponseMode],
                                                       hcdHandlers: HcdHandlers[Msg])
    extends Actor.MutableBehavior[HcdMsgNew] {

  implicit val ec: ExecutionContext = ctx.executionContext

  var mode: ComponentResponseMode = Idle
  ctx.self ! Initialize

  override def onMessage(msg: HcdMsgNew): Behavior[HcdMsgNew] =
    (mode, msg) match {
      case (Idle, x: IdleComponentMsg)              ⇒ onIdle(x); Actor.same
      case (_: Initialized, x: InitialComponentMsg) ⇒ onInitial(x); Actor.same
      case (_: Running, x: RunningComponentMsg)     ⇒ runningComponentBehavior
      case _                                        ⇒ println(s"current context=$mode does not handle message=$msg"); Actor.same
    }
  this

  def initialization(): Future[Unit] = async {
    await(hcdHandlers.initialize())
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
      hcdHandlers.onRun()
      val running = Running(ctx.self.upcast)
      mode = running
      hcdHandlers.isOnline = true
      supervisor ! running
  }

  private def onRunning1(x: RunningComponentMsg): Unit = x match {
    case Lifecycle(message) =>
    case DomainHcdMsg(msg)  =>
  }

  private def onRunning(x: RunningComponentMsg): Unit = x match {
    case Lifecycle(message)   => onLifecycle(message)
    case Submit(command)      => hcdHandlers.onSetup(command)
    case DomainHcdMsg(y: Msg) ⇒ hcdHandlers.onDomainMsg(y)
    case DomainHcdMsg(y)      ⇒ println(s"unhandled domain msg: $y")
  }

  private def onLifecycle(message: ToComponentLifecycleMessageNew): Unit = message match {
    case Shutdown =>
      hcdHandlers.onShutdown()
      hcdHandlers.stopChildren()
      supervisor ! ShutdownComplete
    case Restart =>
      hcdHandlers.onRestart()
      mode = Idle
      ctx.self ! Start
    case GoOnline =>
      if (!hcdHandlers.isOnline) {
        hcdHandlers.onGoOnline()
        hcdHandlers.isOnline = true
      }
    case GoOffline =>
      if (hcdHandlers.isOnline) {
        hcdHandlers.onGoOffline()
        hcdHandlers.isOnline = false
      }
    case LifecycleFailureInfo(state, reason) => hcdHandlers.onLifecycleFailureInfo(state, reason)
  }
}
