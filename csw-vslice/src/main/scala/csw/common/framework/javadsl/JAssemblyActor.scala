package csw.common.framework.javadsl

import java.util.Optional
import java.util.concurrent.CompletableFuture

import akka.actor.Scheduler
import akka.typed.javadsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior, Signal}
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.ccs.{CommandStatus, Validation}
import csw.common.framework.models.AssemblyComponentLifecycleMessage.{Initialized, Running}
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models.InitialAssemblyMsg.Run
import csw.common.framework.models.RunningAssemblyMsg.{DomainAssemblyMsg, Lifecycle, Oneway, Submit}
import csw.common.framework.models._
import csw.common.framework.scaladsl.AssemblyActor.Mode
import csw.param.Parameters
import csw.param.Parameters.{Observe, Setup}
import csw.services.logging.javadsl.JComponentLoggerTypedActor

import scala.async.Async.{async, await}
import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.reflect.ClassTag

abstract class JAssemblyActor[Msg <: DomainMsg: ClassTag](ctx: ActorContext[AssemblyMsg],
                                                          assemblyInfo: AssemblyInfo,
                                                          supervisor: ActorRef[AssemblyComponentLifecycleMessage])
    extends JComponentLoggerTypedActor[AssemblyMsg](ctx) {

  val runningHcd: Option[HcdComponentLifecycleMessage.Running] = None

  var mode: Mode = _

  implicit val scheduler: Scheduler = ctx.asScala.system.scheduler
  implicit val executionContext     = ctx.asScala.executionContext

  def jInitialize(): CompletableFuture[Void]
  def jOnRun(): Void
  def jSetup(s: Parameters.Setup,
             commandOriginator: Optional[ActorRef[CommandStatus.CommandResponse]]): Validation.Validation
  def jObserve(o: Parameters.Observe, replyTo: Optional[ActorRef[CommandStatus.CommandResponse]]): Validation.Validation
  def jOnDomainMsg(msg: Msg): Void
  def jOnLifecycle(message: ToComponentLifecycleMessage): Void

  async {
    await(jInitialize().toScala)
    supervisor ! Initialized(ctx.asScala.self)
    mode = Mode.Initial
  }

  override def createReceive: Actor.Receive[AssemblyMsg] = return new Actor.Receive[AssemblyMsg]() {
    override def receiveMessage(msg: AssemblyMsg): Behavior[AssemblyMsg] = onMessage(msg)

    override def receiveSignal(sig: Signal): Behavior[AssemblyMsg] = Actor.unhandled
  }

  def onMessage(msg: AssemblyMsg): Behavior[AssemblyMsg] = {
    (mode, msg) match {
      case (Mode.Initial, x: InitialAssemblyMsg) ⇒ handleInitial(x)
      case (Mode.Running, x: RunningAssemblyMsg) ⇒ handleRunning(x)
      case _                                     ⇒ println(s"current context=$mode does not handle message=$msg")
    }
    this
  }

  def handleInitial(x: InitialAssemblyMsg): Unit = x match {
    case Run(replyTo) =>
      jOnRun()
      mode = Mode.Running
      replyTo ! Running(ctx.asScala.self)
  }

  def handleRunning(x: RunningAssemblyMsg): Unit = x match {
    case Lifecycle(message)               => jOnLifecycle(message)
    case Submit(command, replyTo)         => onSubmit(command, replyTo)
    case Oneway(command, replyTo)         ⇒ onOneWay(command, replyTo)
    case DomainAssemblyMsg(diagMode: Msg) ⇒ jOnDomainMsg(diagMode)
  }

  def onSubmit(command: Parameters.ControlCommand, replyTo: ActorRef[CommandResponse]): Unit = command match {
    case si: Setup   => setupSubmit(si, oneway = false, replyTo)
    case oi: Observe => observeSubmit(oi, oneway = false, replyTo)
  }

  def onOneWay(command: Parameters.ControlCommand, replyTo: ActorRef[CommandResponse]): Unit = command match {
    case sca: Setup   => setupSubmit(sca, oneway = true, replyTo)
    case oca: Observe => observeSubmit(oca, oneway = true, replyTo)
  }

  private def setupSubmit(s: Setup, oneway: Boolean, replyTo: ActorRef[CommandResponse]): Unit = {
    val completionReplyTo       = if (oneway) None else Some(replyTo)
    val validation              = jSetup(s, completionReplyTo.asJava)
    val validationCommandResult = CommandStatus.validationAsCommandStatus(validation)
    replyTo ! validationCommandResult
  }

  private def observeSubmit(o: Observe, oneway: Boolean, replyTo: ActorRef[CommandResponse]): Unit = {
    val completionReplyTo = if (oneway) None else Some(replyTo)
    val validation        = jObserve(o, completionReplyTo.asJava)

    val validationCommandResult = CommandStatus.validationAsCommandStatus(validation)
    replyTo ! validationCommandResult
  }
}
