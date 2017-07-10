package csw.vslice.hcd.mutable

import akka.actor.Scheduler
import akka.typed.scaladsl.AskPattern.Askable
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import akka.util.Timeout
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState
import csw.param.UnitsOfMeasure.encoder
import csw.vslice.hcd.messages.AxisRequest._
import csw.vslice.hcd.messages.AxisResponse._
import csw.vslice.hcd.messages.FromComponentLifecycleMessage.Initialized
import csw.vslice.hcd.messages.Initial.{HcdResponse, Run, ShutdownComplete}
import csw.vslice.hcd.messages.Running._
import csw.vslice.hcd.messages.ToComponentLifecycleMessage.{
  DoRestart,
  DoShutdown,
  LifecycleFailureInfo,
  RunningOffline
}
import csw.vslice.hcd.messages.TromboneEngineering.{GetAxisConfig, GetAxisStats, GetAxisUpdate, GetAxisUpdateNow}
import csw.vslice.hcd.messages.{FromComponentLifecycleMessage, ToComponentLifecycleMessage, _}
import csw.vslice.hcd.models.AxisConfig
import csw.vslice.hcd.mutable.MutableTromboneHcd.Context

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

object MutableTromboneHcd {
  def behavior(supervisor: ActorRef[FromComponentLifecycleMessage]): Behavior[Nothing] = {
    val beh: Behavior[PubSub[CurrentState]] = Actor.mutable(ctx ⇒ new PubSubActor[CurrentState](new PubSubKey)(ctx))
    Actor.mutable[TromboneMsg](ctx => new MutableTromboneHcd(ctx)(supervisor, ctx.spawnAnonymous(beh))).narrow
  }

  sealed trait Context
  object Context {
    case object Initial extends Context
    case object Running extends Context
  }
}

class MutableTromboneHcd(ctx: ActorContext[TromboneMsg])(supervisor: ActorRef[FromComponentLifecycleMessage],
                                                         pubSubRef: ActorRef[PubSub[CurrentState]])
    extends Actor.MutableBehavior[TromboneMsg] {

  implicit val timeout              = Timeout(2.seconds)
  implicit val scheduler: Scheduler = ctx.system.scheduler
  import ctx.executionContext

  var current: AxisUpdate                 = _
  var stats: AxisStatistics               = _
  var tromboneAxis: ActorRef[AxisRequest] = _
  var axisConfig: AxisConfig              = _
  var context: Context                    = Context.Initial

  async {
    axisConfig = await(getAxisConfig)
    tromboneAxis = setupAxis(axisConfig)
    current = await(tromboneAxis ? InitialState)
    stats = await(tromboneAxis ? GetStatistics)
    supervisor ! Initialized(ctx.self)
  }

  override def onMessage(msg: TromboneMsg): Behavior[TromboneMsg] = {
    (msg, context) match {
      case (x: Initial, Context.Initial) ⇒ handleInitial(x)
      case (x: Running, Context.Running) ⇒ handleRuning(x)
      case _                             ⇒ println(s"current context=$context does not handle message=$msg")
    }
    this
  }

  def handleInitial(x: Initial): Unit = x match {
    case Run(replyTo) =>
      println("received Running")
      context = Context.Running
      replyTo ! HcdResponse(ctx.self)
    case ShutdownComplete =>
      println("received Shutdown complete during Initial context")
  }

  def handleRuning(x: Running): Unit = x match {
    case ShutdownComplete       => println("received Shutdown complete during Initial state")
    case Lifecycle(message)     => handleLifecycle(message)
    case Submit(command)        => handleSetup(command)
    case GetPubSubActorRef      => PubSubRef(pubSubRef)
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
    import csw.vslice.hcd.models.TromboneHcdState._
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
      import csw.vslice.hcd.models.TromboneHcdState._
      val axisConfigState = defaultConfigState.madd(
        lowLimitKey    -> axisConfig.lowLimit,
        lowUserKey     -> axisConfig.lowUser,
        highUserKey    -> axisConfig.highUser,
        highLimitKey   -> axisConfig.highLimit,
        homeValueKey   -> axisConfig.home,
        startValueKey  -> axisConfig.startPosition,
        stepDelayMSKey -> axisConfig.stepDelayMS
      )
      pubSubRef ! PubSub.Publish(axisConfigState)
  }

  def handleAxisResponse(axisResponse: AxisResponse): Unit = axisResponse match {
    case AxisStarted          =>
    case AxisFinished(newRef) =>
    case au @ AxisUpdate(axisName, axisState, current1, inLowLimit, inHighLimit, inHomed) =>
      import csw.vslice.hcd.models.TromboneHcdState._
      val tromboneAxisState = defaultAxisState.madd(
        positionKey    -> current1 withUnits encoder,
        stateKey       -> axisState.toString,
        inLowLimitKey  -> inLowLimit,
        inHighLimitKey -> inHighLimit,
        inHomeKey      -> inHomed
      )
      pubSubRef ! PubSub.Publish(tromboneAxisState)
      current = au
    case AxisFailure(reason) =>
    case as: AxisStatistics =>
      import csw.vslice.hcd.models.TromboneHcdState._
      val tromboneStats = defaultStatsState.madd(
        datumCountKey   -> as.initCount,
        moveCountKey    -> as.moveCount,
        limitCountKey   -> as.limitCount,
        homeCountKey    -> as.homeCount,
        successCountKey -> as.successCount,
        failureCountKey -> as.failureCount,
        cancelCountKey  -> as.cancelCount
      )
      pubSubRef ! PubSub.Publish(tromboneStats)
      stats = as
  }

  private def getAxisConfig: Future[AxisConfig] = ???

  private def setupAxis(ac: AxisConfig): ActorRef[AxisRequest] = {
    ctx.spawnAnonymous(MutableAxisSimulator.behaviour(ac, Some(ctx.self)))
  }
}
