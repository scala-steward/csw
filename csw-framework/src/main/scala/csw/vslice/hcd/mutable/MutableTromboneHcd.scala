package csw.vslice.hcd.mutable

import akka.NotUsed
import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState
import csw.param.UnitsOfMeasure.encoder
import csw.vslice.framework._
import csw.vslice.hcd.messages.AxisRequest._
import csw.vslice.hcd.messages.AxisResponse._
import csw.vslice.framework.ToComponentLifecycleMessage._
import csw.vslice.hcd.messages.TromboneEngineering.{GetAxisConfig, GetAxisStats, GetAxisUpdate, GetAxisUpdateNow}
import csw.vslice.hcd.messages.{AxisRequest, AxisResponse, TromboneEngineering, TromboneMsg}
import csw.vslice.hcd.models.AxisConfig

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

object MutableTromboneHcd extends HcdActorFactory[TromboneMsg] with DomainMsgFactory[TromboneMsg] {
  override protected def make(
      supervisor: ActorRef[FromComponentLifecycleMessage],
      pubSubRef: ActorRef[PubSub[CurrentState]]
  )(ctx: ActorContext[HcdMsg]): HcdActor[TromboneMsg] = new MutableTromboneHcd(ctx)(supervisor, pubSubRef)
}

class MutableTromboneHcd(ctx: ActorContext[HcdMsg])(supervisor: ActorRef[FromComponentLifecycleMessage],
                                                    pubSubRef: ActorRef[PubSub[CurrentState]])
    extends HcdActor[TromboneMsg](ctx)(supervisor, pubSubRef) {

  implicit val timeout              = Timeout(2.seconds)
  implicit val scheduler: Scheduler = ctx.system.scheduler
  import ctx.executionContext

  var current: AxisUpdate                 = _
  var stats: AxisStatistics               = _
  var tromboneAxis: ActorRef[AxisRequest] = _
  var axisConfig: AxisConfig              = _

  override def preStart(): Future[NotUsed] = async {
    axisConfig = await(getAxisConfig)
    tromboneAxis = ctx.spawnAnonymous(MutableAxisSimulator.behaviour(axisConfig, Some(wrapper)))
    current = await(tromboneAxis ? InitialState)
    stats = await(tromboneAxis ? GetStatistics)
    NotUsed
  }

  override def onRun(): Unit = println("received Running")

  override def onShutdown(): Unit = println("received Shutdown complete during Initial context")

  override def onShutdownComplete(): Unit = println("received Shutdown complete during Initial state")

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

  def handleDomainMsg(tromboneMsg: TromboneMsg): Unit = tromboneMsg match {
    case x: TromboneEngineering => handleEng(x)
    case x: AxisResponse        => handleAxisResponse(x)
  }

  private def handleEng(tromboneEngineering: TromboneEngineering): Unit = tromboneEngineering match {
    case GetAxisStats              => tromboneAxis ! GetStatistics(wrapper)
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

  private def handleAxisResponse(axisResponse: AxisResponse): Unit = axisResponse match {
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
}
