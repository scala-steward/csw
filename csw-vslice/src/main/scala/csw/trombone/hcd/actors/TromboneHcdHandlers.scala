package csw.trombone.hcd.actors

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.PubSub.PublisherMessage
import csw.messages._
import csw.messages.ccs.{Validation, Validations}
import csw.messages.ccs.Validations.Valid
import csw.messages.ccs.commands.{ControlCommand, Observe, Setup}
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.params.models.Units.encoder
import csw.messages.params.states.CurrentState
import csw.services.location.scaladsl.LocationService
import csw.trombone.hcd.AxisRequest._
import csw.trombone.hcd.AxisResponse._
import csw.trombone.hcd.TromboneEngineering.{GetAxisConfig, GetAxisStats, GetAxisUpdate, GetAxisUpdateNow}
import csw.trombone.hcd._

import scala.async.Async._
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContextExecutor, Future}

class TromboneHcdBehaviorFactory extends ComponentBehaviorFactory[TromboneMessage] {
  override def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PublisherMessage[CurrentState]],
      locationService: LocationService
  ): ComponentHandlers[TromboneMessage] =
    TromboneHcdHandlers(ctx, componentInfo, pubSubRef, locationService, None, None, None, None)
}

case class TromboneHcdHandlers(ctx: ActorContext[ComponentMessage],
                               componentInfo: ComponentInfo,
                               pubSubRef: ActorRef[PublisherMessage[CurrentState]],
                               locationService: LocationService,
                               axisConfig: Option[AxisConfig],
                               tromboneAxis: Option[ActorRef[AxisRequest]],
                               current: Option[AxisUpdate],
                               stats: Option[AxisStatistics])
    extends ComponentHandlers[TromboneMessage](ctx, componentInfo, pubSubRef, locationService) {

  implicit val timeout: Timeout             = Timeout(2.seconds)
  implicit val scheduler: Scheduler         = ctx.system.scheduler
  implicit val ec: ExecutionContextExecutor = ctx.executionContext

  override def initialize(): Future[ComponentHandlers[TromboneMessage]] = async {
    val axisConfig   = await(getAxisConfig)
    val tromboneAxis = ctx.spawnAnonymous(AxisSimulator.behavior(axisConfig, Some(ctx.self)))
    val current      = await(tromboneAxis ? InitialState)
    val stats        = await(tromboneAxis ? GetStatistics)
    this.copy(axisConfig = Some(axisConfig),
              tromboneAxis = Some(tromboneAxis),
              current = Some(current),
              stats = Some(stats))
  }
  override def onShutdown(): Future[Unit] = {
    Future.successful(println("shutdown complete during Running context"))
  }

  override def onGoOffline(): Unit = println("Received running offline")

  override def onGoOnline(): Unit = println("Received running offline")

  private def onSetup(sc: Setup): Unit = {
    import csw.trombone.hcd.TromboneHcdState._
    println(s"Trombone process received sc: $sc")

    sc.prefix match {
      case `axisMoveCK` =>
        tromboneAxis.foreach(_ ! Move(sc(positionKey).head, diagFlag = true))
      case `axisDatumCK` =>
        println("Received Datum")
        tromboneAxis.foreach(_ ! Datum)
      case `axisHomeCK` =>
        tromboneAxis.foreach(_ ! Home)
      case `axisCancelCK` =>
        tromboneAxis.foreach(_ ! CancelMove)
      case x => println(s"Unknown config key $x")
    }
  }

  override def onSubmit(controlCommand: ControlCommand, replyTo: ActorRef[CommandResponse]): Validation = {
    val validation = controlCommand match {
      case setup: Setup     => ParamValidation.validateSetup(setup)
      case observe: Observe => ParamValidation.validateObserve(observe)
    }
    if (validation == Valid)
      onSetup(controlCommand.asInstanceOf[Setup])
    validation
  }
  override def onOneway(controlCommand: ControlCommand): Validation = Validations.Valid

  def onDomainMsg(tromboneMsg: TromboneMessage): ComponentHandlers[TromboneMessage] = tromboneMsg match {
    case x: TromboneEngineering => onEngMsg(x)
    case x: AxisResponse        => onAxisResponse(x)
  }

  private def onEngMsg(tromboneEngineering: TromboneEngineering): ComponentHandlers[TromboneMessage] =
    tromboneEngineering match {
      case GetAxisStats =>
        tromboneAxis.foreach(_ ! GetStatistics(ctx.self))
        this
      case GetAxisUpdate =>
        tromboneAxis.foreach(_ ! PublishAxisUpdate)
        this
      case GetAxisUpdateNow(replyTo) =>
        current.foreach(replyTo ! _)
        this
      case GetAxisConfig =>
        import csw.trombone.hcd.TromboneHcdState._
        val axisConfigState: Option[CurrentState] = axisConfig.map(
          ac ⇒
            defaultConfigState.madd(
              lowLimitKey    -> ac.lowLimit,
              lowUserKey     -> ac.lowUser,
              highUserKey    -> ac.highUser,
              highLimitKey   -> ac.highLimit,
              homeValueKey   -> ac.home,
              startValueKey  -> ac.startPosition,
              stepDelayMSKey -> ac.stepDelayMS
          )
        )
        axisConfigState.foreach(state ⇒ pubSubRef ! PubSub.Publish(state))
        this
    }

  private def onAxisResponse(axisResponse: AxisResponse): ComponentHandlers[TromboneMessage] = axisResponse match {
    case AxisStarted          => this
    case AxisFinished(newRef) => this
    case au @ AxisUpdate(axisName, axisState, current1, inLowLimit, inHighLimit, inHomed) =>
      import csw.trombone.hcd.TromboneHcdState._
      val tromboneAxisState = defaultAxisState.madd(
        positionKey    -> current1 withUnits encoder,
        stateKey       -> axisState.toString,
        inLowLimitKey  -> inLowLimit,
        inHighLimitKey -> inHighLimit,
        inHomeKey      -> inHomed
      )
      pubSubRef ! PubSub.Publish(tromboneAxisState)
      this.copy(current = Some(au))
    case AxisFailure(reason) => this
    case as: AxisStatistics =>
      import csw.trombone.hcd.TromboneHcdState._
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
      this.copy(stats = Some(as))
  }

  private def getAxisConfig: Future[AxisConfig] = ???

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): ComponentHandlers[TromboneMessage] = ???
}
