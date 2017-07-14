package csw.vslice.assembly

import java.time.Instant

import akka.actor.Cancellable
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.param.StateVariable.CurrentState
import csw.vslice.assembly.DiagPublisher.DiagPublisherMessages._
import csw.vslice.assembly.DiagPublisher._
import csw.vslice.assembly.TrombonePublisher.{AxisStateUpdate, AxisStatsUpdate, TrombonePublisherMsg}
import csw.vslice.framework.FromComponentLifecycleMessage.Running
import csw.vslice.framework.PubSub
import csw.vslice.framework.RunningHcdMsg.DomainHcdMsg
import csw.vslice.hcd.models.TromboneEngineering.GetAxisStats
import csw.vslice.hcd.models.TromboneHcdState

import scala.concurrent.duration.DurationDouble

class DiagPublisher(assemblyContext: AssemblyContext,
                    runningIn: Option[Running],
                    eventPublisher: Option[ActorRef[TrombonePublisherMsg]],
                    ctx: ActorContext[DiagPublisherMessages])
    extends MutableBehavior[DiagPublisherMessages] {

  val currentStateAdapter: ActorRef[CurrentState] = ctx.spawnAdapter(CurrentStateE)

  var stateMessageCounter: Int = 0
  var running: Option[Running] = runningIn
  var context: Context         = _
  var cancelToken: Cancellable = _

  running.foreach(_.pubSubRef ! PubSub.Subscribe(currentStateAdapter))

  override def onMessage(msg: DiagPublisherMessages): Behavior[DiagPublisherMessages] = {
    context match {
      case Context.Operations ⇒ operationsReceive(msg)
      case Context.Diagnostic ⇒ diagnosticReceive(msg)
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
      context = Context.Diagnostic

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
      context = Context.Operations

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

  sealed trait Context
  object Context {
    case object Operations extends Context
    case object Diagnostic extends Context
  }

  sealed trait DiagPublisherMessages
  object DiagPublisherMessages {
    final case class TimeForAxisStats(periodInseconds: Int)      extends DiagPublisherMessages
    final case object DiagnosticState                            extends DiagPublisherMessages
    final case object OperationsState                            extends DiagPublisherMessages
    final case class CurrentStateE(cs: CurrentState)             extends DiagPublisherMessages
    final case class UpdateTromboneHcd(running: Option[Running]) extends DiagPublisherMessages
  }

  val diagnosticSkipCount       = 2
  val operationsSkipCount       = 5
  val diagnosticAxisStatsPeriod = 1
}
