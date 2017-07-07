package csw.framework.mutable

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.framework.messages.AxisRequest._
import csw.framework.messages.AxisResponse._
import csw.framework.messages.Initial.{Run, ShutdownComplete}
import csw.framework.messages.Running.{Lifecycle, Publish, Submit}
import csw.framework.messages.ToComponentLifecycleMessage.{DoRestart, DoShutdown, LifecycleFailureInfo, RunningOffline}
import csw.framework.messages.TromboneEngineering.{GetAxisConfig, GetAxisStats, GetAxisUpdate, GetAxisUpdateNow}
import csw.framework.messages.{FromComponentLifecycleMessage, ToComponentLifecycleMessage, _}
import csw.framework.models.AxisConfig
import csw.framework.mutable.MutableTromboneHcd.Context
import csw.param.Parameters.Setup
import csw.param.UnitsOfMeasure.encoder

object MutableTromboneHcd {
  def behavior(supervisor: ActorRef[Any]): Behavior[TromboneMsg] =
    Actor.mutable(ctx ⇒ new MutableTromboneHcd(ctx)(supervisor))

  sealed trait Context
  object Context {
    case object Initial extends Context
    case object Running extends Context
  }
}

class MutableTromboneHcd(ctx: ActorContext[TromboneMsg])(supervisor: ActorRef[Any])
    extends Actor.MutableBehavior[TromboneMsg] {

  var current: AxisUpdate                 = _
  var stats: AxisStatistics               = _
  var tromboneAxis: ActorRef[AxisRequest] = _
  var axisConfig: AxisConfig              = _
  var context: Context                    = Context.Initial

  override def onMessage(msg: TromboneMsg): Behavior[TromboneMsg] = {
    (msg, context) match {
      case (x: Initial, Context.Initial) ⇒ handleInitial(x)
      case (x: Running, Context.Running) ⇒ handleRuning(x)
      case _                             ⇒ println(s"current context=$context does not handle message=$msg")
    }
    this
  }

  def handleInitial(x: Initial): Unit = x match {
    case Run =>
      println("received Running")
      context = Context.Running
    case ShutdownComplete =>
      println("received Shutdown complete during Initial context")
  }

  def handleRuning(x: Running): Unit = x match {
    case ShutdownComplete       => println("received Shutdown complete during Initial state")
    case Lifecycle(message)     => handleLifecycle(message)
    case Submit(command)        => handleSetup(command)
    case Publish(currentState)  =>
    case y: TromboneEngineering => handleEng(y)
    case y: AxisResponse        => handleAxisResponse(y)
  }

  def handleLifecycle(x: ToComponentLifecycleMessage): Unit = x match {
    case DoShutdown =>
      println("Received doshutdown")
      supervisor ! FromComponentLifecycleMessage.ShutdownComplete
    case DoRestart                                      => println("Received dorestart")
    case ToComponentLifecycleMessage.Running            => println("Received running")
    case RunningOffline                                 => println("Received running offline")
    case LifecycleFailureInfo(state, reason)            => println(s"Received failed state: $state for reason: $reason")
    case FromComponentLifecycleMessage.ShutdownComplete => println("shutdown complete during Running context")
  }

  def handleSetup(sc: Setup): Unit = {
    import csw.framework.models.TromboneHcdState._
    println(s"Trombone process received sc: $sc")

    sc.prefix match {
      case `axisMoveCK` =>
        tromboneAxis ! Move(sc(positionKey).head, diagFlag = true)
      case `axisDatumCK` =>
        println("Received Datum")
        tromboneAxis ! Datum
      case `axisHomeCK` =>
        tromboneAxis ! Home
      case `axisCancelCK` =>
        tromboneAxis ! CancelMove
      case x => println(s"Unknown config key $x")
    }
  }

  def handleEng(tromboneEngineering: TromboneEngineering): Unit = tromboneEngineering match {
    case GetAxisStats              => tromboneAxis ! GetStatistics(ctx.self)
    case GetAxisUpdate             => tromboneAxis ! PublishAxisUpdate
    case GetAxisUpdateNow(replyTo) => replyTo ! current
    case GetAxisConfig =>
      import csw.framework.models.TromboneHcdState._
      val axisConfigState = defaultConfigState.madd(
        lowLimitKey    -> axisConfig.lowLimit,
        lowUserKey     -> axisConfig.lowUser,
        highUserKey    -> axisConfig.highUser,
        highLimitKey   -> axisConfig.highLimit,
        homeValueKey   -> axisConfig.home,
        startValueKey  -> axisConfig.startPosition,
        stepDelayMSKey -> axisConfig.stepDelayMS
      )
    //TODO: PubSub
  }

  def handleAxisResponse(axisResponse: AxisResponse): Unit = axisResponse match {
    case AxisStarted          =>
    case AxisFinished(newRef) =>
    case au @ AxisUpdate(axisName, axisState, current1, inLowLimit, inHighLimit, inHomed) =>
      import csw.framework.models.TromboneHcdState._
      val tromboneAxisState = defaultAxisState.madd(
        positionKey    -> current1 withUnits encoder,
        stateKey       -> axisState.toString,
        inLowLimitKey  -> inLowLimit,
        inHighLimitKey -> inHighLimit,
        inHomeKey      -> inHomed
      )
      //TODO: PubSub
      current = au
    case AxisFailure(reason) =>
    case as: AxisStatistics =>
      import csw.framework.models.TromboneHcdState._
      val tromboneStats = defaultStatsState.madd(
        datumCountKey   -> as.initCount,
        moveCountKey    -> as.moveCount,
        limitCountKey   -> as.limitCount,
        homeCountKey    -> as.homeCount,
        successCountKey -> as.successCount,
        failureCountKey -> as.failureCount,
        cancelCountKey  -> as.cancelCount
      )
      //TODO: PubSub
      stats = as
  }
}
