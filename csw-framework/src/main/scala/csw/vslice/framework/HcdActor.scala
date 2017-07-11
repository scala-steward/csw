package csw.vslice.framework

import akka.NotUsed
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState
import csw.vslice.framework.FromComponentLifecycleMessage.Initialized
import csw.vslice.framework.HcdActor.Context
import csw.vslice.framework.InitialHcdMsg.{Run, Running, ShutdownComplete}
import csw.vslice.framework.RunningHcdMsg._

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.reflect.ClassTag

object HcdActor {
  sealed trait Context
  object Context {
    case object Initial extends Context
    case object Running extends Context
  }
}

abstract class HcdActor[Msg <: DomainMsg: ClassTag](ctx: ActorContext[HcdMsg])(
    supervisor: ActorRef[FromComponentLifecycleMessage],
    pubSubRef: ActorRef[PubSub[CurrentState]]
) extends Actor.MutableBehavior[HcdMsg] {

  val domainRef: ActorRef[Msg] = ctx.spawnAdapter { x: Msg ⇒
    DomainHcdMsg(x)
  }

  import ctx.executionContext

  var context: Context = _

  def preStart(): Future[NotUsed]

  async {
    await(preStart())
    supervisor ! Initialized(ctx.self)
    context = Context.Initial
  }

  override def onMessage(msg: HcdMsg): Behavior[HcdMsg] = {
    (context, msg) match {
      case (Context.Initial, x: InitialHcdMsg) ⇒ handleInitial(x)
      case (Context.Running, x: RunningHcdMsg) ⇒ handleRunning(x)
      case _                                   ⇒ println(s"current context=$context does not handle message=$msg")
    }
    this
  }

  def handleInitial(x: InitialHcdMsg): Unit = x match {
    case Run(replyTo) =>
      onRun()
      context = Context.Running
      replyTo ! Running(ctx.self, pubSubRef)
    case ShutdownComplete =>
      onShutdown()
  }

  private def handleRunning(x: RunningHcdMsg): Unit = x match {
    case ShutdownComplete     => onShutdownComplete()
    case Lifecycle(message)   => handleLifecycle(message)
    case Submit(command)      => handleSetup(command)
    case DomainHcdMsg(y: Msg) ⇒ handleDomainMsg(y)
    case DomainHcdMsg(y)      ⇒ println(s"unhandled domain msg: $y")
  }

  def onRun(): Unit
  def onShutdown(): Unit
  def onShutdownComplete(): Unit
  def handleLifecycle(x: ToComponentLifecycleMessage): Unit
  def handleSetup(sc: Setup): Unit
  def handleDomainMsg(msg: Msg): Unit
}
