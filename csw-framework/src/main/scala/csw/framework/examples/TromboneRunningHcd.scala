package csw.framework.examples

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.framework.examples.SingleAxisSimulator._
import csw.framework.examples.TromboneHcdMessages.{W1, W2}
import csw.framework.lifecycle.ToComponentLifecycleMessage

sealed trait TromboneEngineering
object TromboneEngineering {
  case object GetAxisStats                           extends TromboneEngineering
  case object GetAxisUpdate                          extends TromboneEngineering
  case object GetAxisUpdateNow                       extends TromboneEngineering
  case object GetAxisConfig                          extends TromboneEngineering
  case class Payload(axisStatistics: AxisStatistics) extends TromboneEngineering

  case class TromboneState(current: AxisUpdate, stats: AxisStatistics, tromboneAxis: ActorRef[IdleMessage])
}

import csw.framework.examples.HcdRunningBehavior._
import csw.framework.examples.TromboneEngineering._

sealed trait TromboneHcdMessages
object TromboneHcdMessages {
  case class W1(tromboneEngineering: TromboneEngineering) extends TromboneHcdMessages
  case class W2(axisResponse: AxisResponse)               extends TromboneHcdMessages
}

object TromboneRunningHcd extends HcdRunningBehavior[TromboneHcdMessages, TromboneState] {
  override protected def domainSpecific(
      state: TromboneState,
      ctx: ActorContext[RunningMessage[TromboneHcdMessages]],
      msg: TromboneHcdMessages,
      loop: (TromboneState) ⇒ Behavior[RunningMessage[TromboneHcdMessages]]
  ): Behavior[RunningMessage[TromboneHcdMessages]] = msg match {
    case W1(tromboneEngineering) => w1(state, ctx, tromboneEngineering, loop)
    case W2(axisResponse)        => w2(state, ctx, axisResponse, loop)
  }

  private def w1(
      state: TromboneState,
      ctx: ActorContext[RunningMessage[TromboneHcdMessages]],
      msg: TromboneEngineering,
      loop: (TromboneState) ⇒ Behavior[RunningMessage[TromboneHcdMessages]]
  ): Behavior[RunningMessage[TromboneHcdMessages]] = msg match {
    case GetAxisStats =>
      val replyTo = ctx.self.narrow[DomainSpecific[W2]]
      state.tromboneAxis ! IdleAxisRequest(GetStatistics(replyTo))
      Actor.same
    case GetAxisUpdate    =>
    case GetAxisUpdateNow =>
    case GetAxisConfig    =>
  }

  private def w2(
      state: TromboneState,
      ctx: ActorContext[RunningMessage[TromboneHcdMessages]],
      msg: AxisResponse,
      loop: (TromboneState) ⇒ Behavior[RunningMessage[TromboneHcdMessages]]
  ): Behavior[RunningMessage[TromboneHcdMessages]] = msg match {
    case AxisStarted         =>
    case AxisFinished        =>
    case x: AxisUpdate       =>
    case AxisFailure(reason) =>
    case x: AxisStatistics   =>
  }

  override protected def lifecycle(
      state: TromboneState,
      ctx: ActorContext[RunningMessage[TromboneHcdMessages]],
      msg: ToComponentLifecycleMessage,
      loop: (TromboneState) ⇒ Behavior[RunningMessage[TromboneHcdMessages]]
  ): Behavior[RunningMessage[TromboneEngineering]] = ???
}
