package csw.trombone.assembly.actors

import java.time.Instant

import akka.actor.Cancellable
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.param.StateVariable.CurrentState
import csw.trombone.assembly.DiagPublisherMessages._
import csw.trombone.assembly.actors.DiagPublisher._
import csw.trombone.assembly.TrombonePublisherMsg.{AxisStateUpdate, AxisStatsUpdate}
import csw.trombone.assembly.actors.DiagPublisher.Mode
import csw.trombone.assembly.{AssemblyContext, DiagPublisherMessages, TrombonePublisherMsg}
import csw.trombone.framework.HcdComponentLifecycleMessage.Running
import csw.trombone.framework.PubSub
import csw.trombone.framework.RunningHcdMsg.DomainHcdMsg
import csw.trombone.hcd.models.TromboneEngineering.GetAxisStats
import csw.trombone.hcd.models.TromboneHcdState

import scala.concurrent.duration.DurationDouble

class DiagPublisher(assemblyContext: AssemblyContext,
                    runningIn: Option[Running],
                    eventPublisher: Option[ActorRef[TrombonePublisherMsg]],
                    ctx: ActorContext[DiagPublisherMessages])
    extends MutableBehavior[DiagPublisherMessages] {

  val currentStateAdapter: ActorRef[CurrentState] = ctx.spawnAdapter(CurrentStateE)

  var stateMessageCounter: Int = 0
  var running: Option[Running] = runningIn
  var context: Mode            = _
  var cancelToken: Cancellable = _

  running.foreach(_.pubSubRef ! PubSub.Subscribe(currentStateAdapter))

  override def onMessage(msg: DiagPublisherMessages): Behavior[DiagPublisherMessages] = {
    context match {
      case Mode.Operations ⇒ operationsReceive(msg)
      case Mode.Diagnostic ⇒ diagnosticReceive(msg)
    }
    this
  }

  def operationsReceive(msg: DiagPublisherMessages): Unit = msg match {
    case CurrentStateE(cs) if cs.prefix == TromboneHcdState.axisStateCK =>
      if (stateMessageCounter % operationsSkipCount == 0) {
        publishStateUpdate(cs)
        stateMessageCounter = stateMessageCounter + 1
      }

    case CurrentStateE(cs) if cs.prefix == TromboneHcdState.axisStatsCK => // No nothing
    case TimeForAxisStats(_)                                            => // Do nothing, here so it doesn't make an error
    case OperationsState                                                => // Already in operations mode

    case DiagnosticState =>
      val cancelToken: Cancellable = ctx.schedule(
        Instant.now().plusSeconds(diagnosticAxisStatsPeriod).toEpochMilli.millis,
        ctx.self,
        TimeForAxisStats(diagnosticAxisStatsPeriod)
      )
      this.cancelToken = cancelToken
      context = Mode.Diagnostic

    case UpdateTromboneHcd(maybeRunning) =>
      this.running = maybeRunning
  }

  def diagnosticReceive(msg: DiagPublisherMessages): Unit = msg match {
    case CurrentStateE(cs) if cs.prefix == TromboneHcdState.axisStateCK =>
      if (stateMessageCounter % diagnosticSkipCount == 0) {
        publishStateUpdate(cs)
        stateMessageCounter = stateMessageCounter + 1
      }

    case CurrentStateE(cs) if cs.prefix == TromboneHcdState.axisStatsCK =>
      publishStatsUpdate(cs)

    case TimeForAxisStats(periodInSeconds) =>
      running.foreach(_.hcdRef ! DomainHcdMsg(GetAxisStats))
      val canceltoken: Cancellable =
        ctx.schedule(Instant.now().plusSeconds(periodInSeconds).toEpochMilli.millis,
                     ctx.self,
                     TimeForAxisStats(periodInSeconds))
      this.cancelToken = canceltoken

    case DiagnosticState => // Do nothing, already in this mode

    case OperationsState =>
      cancelToken.cancel
      context = Mode.Operations

    case UpdateTromboneHcd(maybeRunning) =>
      running = maybeRunning
  }

  private def publishStateUpdate(cs: CurrentState): Unit = {
    eventPublisher.foreach(
      _ ! AxisStateUpdate(
        cs(TromboneHcdState.axisNameKey),
        cs(TromboneHcdState.positionKey),
        cs(TromboneHcdState.stateKey),
        cs(TromboneHcdState.inLowLimitKey),
        cs(TromboneHcdState.inHighLimitKey),
        cs(TromboneHcdState.inHomeKey)
      )
    )
  }

  private def publishStatsUpdate(cs: CurrentState): Unit = {
    eventPublisher.foreach(
      _ ! AxisStatsUpdate(
        cs(TromboneHcdState.axisNameKey),
        cs(TromboneHcdState.datumCountKey),
        cs(TromboneHcdState.moveCountKey),
        cs(TromboneHcdState.homeCountKey),
        cs(TromboneHcdState.limitCountKey),
        cs(TromboneHcdState.successCountKey),
        cs(TromboneHcdState.failureCountKey),
        cs(TromboneHcdState.cancelCountKey)
      )
    )
  }

}

object DiagPublisher {

  def make(assemblyContext: AssemblyContext,
           runningIn: Option[Running],
           eventPublisher: Option[ActorRef[TrombonePublisherMsg]]): Behavior[DiagPublisherMessages] =
    Actor.mutable(ctx ⇒ new DiagPublisher(assemblyContext, runningIn, eventPublisher, ctx))

  sealed trait Mode
  object Mode {
    case object Operations extends Mode
    case object Diagnostic extends Mode
  }

  val diagnosticSkipCount       = 2
  val operationsSkipCount       = 5
  val diagnosticAxisStatsPeriod = 1
}
