package csw.trombone.assembly.actors

import java.io.File

import akka.actor.Scheduler
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.param.Parameters
import csw.param.Parameters.{Observe, Setup}
import csw.trombone.assembly.AssemblyContext.{TromboneCalculationConfig, TromboneControlConfig}
import csw.trombone.assembly.DiagPublisherMessages.{DiagnosticState, OperationsState}
import csw.trombone.assembly.ParamValidation._
import csw.trombone.assembly._
import csw.trombone.assembly.actors.TromboneAssembly.Mode
import csw.trombone.ccs.CommandStatus.CommandResponse
import csw.trombone.ccs.Validation.{Valid, Validation}
import csw.trombone.ccs.{CommandStatus, Validation}
import csw.trombone.framework.Component.AssemblyInfo
import csw.trombone.framework.HcdComponentLifecycleMessage.Running
import csw.trombone.framework.InitialAssemblyMsg.Run
import csw.trombone.framework.RunningAssemblyMsg._
import csw.trombone.framework.ToComponentLifecycleMessage.{DoRestart, DoShutdown, LifecycleFailureInfo, RunningOffline}
import csw.trombone.framework._

import scala.async.Async.{async, await}
import scala.concurrent.Future

class TromboneAssembly(val info: AssemblyInfo, supervisor: ActorRef[Any], ctx: ActorContext[AssemblyMsg])
    extends MutableBehavior[AssemblyMsg] {

  private val tromboneHCD: Option[Running] = ???

  private var diagPublsher: ActorRef[DiagPublisherMessages] = _

  private var commandHandler: ActorRef[TromboneCommandHandlerMsgs] = _

  implicit var ac: AssemblyContext = _

  var mode: Mode = _

  implicit val scheduler: Scheduler = ctx.system.scheduler
  import ctx.executionContext

  def initialize(): Future[Unit] = async {
    val (calculationConfig, controlConfig) = await(getAssemblyConfigs)
    ac = AssemblyContext(info, calculationConfig, controlConfig)

    val eventPublisher = ctx.spawnAnonymous(TrombonePublisher.make(ac))

    commandHandler = ctx.spawnAnonymous(TromboneCommandHandler.make(ac, tromboneHCD, Some(eventPublisher)))

    diagPublsher = ctx.spawnAnonymous(DiagPublisher.make(ac, tromboneHCD, Some(eventPublisher)))
  }

  override def onMessage(msg: AssemblyMsg): Behavior[AssemblyMsg] = {
    (mode, msg) match {
      case (Mode.Initial, x: InitialAssemblyMsg) ⇒ handleInitial(x)
      case (Mode.Running, x: RunningAssemblyMsg) ⇒ handleRunning(x)
      case _                                     ⇒ println(s"current context=$mode does not handle message=$msg")
    }
    this
  }

  def handleInitial(x: InitialAssemblyMsg): Unit = x match {
    case Run(replyTo) =>
      mode = Mode.Running
  }

  def handleRunning(x: RunningAssemblyMsg): Any = x match {
    case Lifecycle(message)       => onLifecycle(message)
    case Submit(command, replyTo) => onSubmit(command, replyTo)
    case Oneway(command, replyTo) ⇒ onOneWay(command, replyTo)
    case DiagMsgs(diagMode)       ⇒ onDiag(diagMode)
  }

  def onDiag(mode: DiagPublisherMessages): Unit = mode match {
    case DiagnosticState => diagPublsher ! DiagnosticState
    case OperationsState => diagPublsher ! OperationsState
    case _               ⇒
  }

  def onLifecycle(message: ToComponentLifecycleMessage): Unit = message match {
    case ShutdownComplete                    ⇒
    case ToComponentLifecycleMessage.Running =>
    case RunningOffline                      => println("Received running offline")
    case DoRestart                           => println("Received dorestart")
    case DoShutdown =>
      println("Received doshutdown")
      tromboneHCD.foreach(
        _.hcdRef ! csw.trombone.framework.RunningHcdMsg
          .Lifecycle(csw.trombone.framework.ToComponentLifecycleMessage.DoShutdown)
      )
      supervisor ! ShutdownComplete
    case LifecycleFailureInfo(state: LifecycleState, reason: String) =>
      println(s"TromboneAssembly received failed lifecycle state: $state for reason: $reason")
  }

  def onSubmit(command: Parameters.ControlCommand, replyTo: ActorRef[CommandResponse]): Any = command match {
    case si: Setup   => setupSubmit(si, oneway = false, replyTo)
    case oi: Observe => observeSubmit(oi, oneway = false, replyTo)
  }

  def onOneWay(command: Parameters.ControlCommand, replyTo: ActorRef[CommandResponse]): Any = command match {
    case sca: Setup   => setupSubmit(sca, oneway = true, replyTo)
    case oca: Observe => observeSubmit(oca, oneway = true, replyTo)
  }

  private def getAssemblyConfigs: Future[(TromboneCalculationConfig, TromboneControlConfig)] = ???

  private def setupSubmit(s: Setup, oneway: Boolean, replyTo: ActorRef[CommandResponse]): Unit = {
    val completionReplyTo       = if (oneway) None else Some(replyTo)
    val validation              = setup(s, completionReplyTo)
    val validationCommandResult = CommandStatus.validationAsCommandStatus(validation)
    replyTo ! validationCommandResult
  }

  private def observeSubmit(o: Observe, oneway: Boolean, replyTo: ActorRef[CommandResponse]): Unit = {
    val completionReplyTo = if (oneway) None else Some(replyTo)
    val validation        = observe(o, completionReplyTo)

    val validationCommandResult = CommandStatus.validationAsCommandStatus(validation)
    replyTo ! validationCommandResult
  }

  def setup(s: Setup, commandOriginator: Option[ActorRef[CommandResponse]]): Validation = {
    val validation = validateOneSetup(s)
    if (validation == Valid) {
      commandHandler ! TromboneCommandHandlerMsgs.Submit(
        s,
        commandOriginator.getOrElse(ctx.spawnAnonymous(Behavior.empty))
      )
    }
    validation
  }

  protected def observe(o: Observe, replyTo: Option[ActorRef[CommandResponse]]): Validation = Validation.Valid
}

object TromboneAssembly {

  val tromboneConfigFile = new File("trombone/tromboneAssembly.conf")
  val resource           = new File("tromboneAssembly.conf")

  def make(assemblyInfo: AssemblyInfo, supervisor: ActorRef[Any]): Behavior[AssemblyMsg] =
    Actor.mutable(ctx ⇒ new TromboneAssembly(assemblyInfo, supervisor, ctx))

  sealed trait TromboneAssemblyMsg
  private[assembly] case object CommandStart       extends TromboneAssemblyMsg
  private[assembly] case object StopCurrentCommand extends TromboneAssemblyMsg

  sealed trait Mode
  object Mode {
    case object Initial extends Mode
    case object Running extends Mode
  }
  //  private val badHCDReference = None
}
