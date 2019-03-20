package csw.framework.internal.component

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, PostStop}
import csw.command.client.CommandResponseManager
import csw.command.client.messages.CommandMessage.{Oneway, Submit, Validate}
import csw.command.client.messages.CommandResponseManagerMessage.AddOrUpdateCommand
import csw.command.client.messages.FromComponentLifecycleMessage.Running
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.TopLevelActorCommonMessage.{TrackingEventReceived, UnderlyingHookFailed}
import csw.command.client.messages.TopLevelActorIdleMessage.Initialize
import csw.command.client.messages._
import csw.command.client.models.framework.LocationServiceUsage.RegisterAndTrackServices
import csw.command.client.models.framework.ToComponentLifecycleMessage
import csw.command.client.models.framework.ToComponentLifecycleMessages.{GoOffline, GoOnline}
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandResponse._

import scala.async.Async.{async, await}
import scala.concurrent.Await
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.util.control.NonFatal

// scalastyle:off method.length
private[framework] object ComponentBehavior {

  /**
   * The Behavior of a component actor, represented as a mutable behavior
   *
   * @param supervisor the actor reference of the supervisor actor which created this component
   * @param lifecycleHandlers the implementation of handlers which defines the domain actions to be performed by this component
   */
  def make(
      supervisor: ActorRef[FromComponentLifecycleMessage],
      lifecycleHandlers: ComponentHandlers,
      cswCtx: CswContext
  ): Behavior[TopLevelActorMessage] =
    Behaviors.setup(ctx ⇒ {
      import cswCtx._
      import ctx.executionContext

      val log: Logger     = loggerFactory.getLogger(ctx)
      val handlersWrapper = new HandlersWrapper(lifecycleHandlers, log)

      var lifecycleState: ComponentLifecycleState = ComponentLifecycleState.Idle

      ctx.self ! Initialize

      /*
       * Defines action for messages which can be received in any [[ComponentLifecycleState]] state
       *
       * @param commonMessage message representing a message received in any lifecycle state
       */
      val onCommon: Behavior[TopLevelActorMessage] =
        Behaviors.receiveMessagePartial {
          case UnderlyingHookFailed(exception) ⇒
            log.error(exception.getMessage, ex = exception)
            throw exception
          case TrackingEventReceived(trackingEvent) ⇒
            lifecycleHandlers.onLocationTrackingEvent(trackingEvent)
            Behaviors.same
        }

      /*
       * Defines action for messages which can be received in [[ComponentLifecycleState.Idle]] state
       *
       * @param idleMessage message representing a message received in [[ComponentLifecycleState.Idle]] state
       */
      val onIdle: Behavior[TopLevelActorMessage] = Behaviors.receiveMessagePartial {
        case Initialize ⇒
          async {
            await(handlersWrapper.initialize(lifecycleState))
            lifecycleState = ComponentLifecycleState.Initialized
            // track all connections in component info for location updates
            if (componentInfo.locationServiceUsage == RegisterAndTrackServices) {
              componentInfo.connections.foreach(
                connection ⇒ {
                  locationService.subscribe(connection, trackingEvent ⇒ ctx.self ! TrackingEventReceived(trackingEvent))
                }
              )
            }
            lifecycleState = ComponentLifecycleState.Running
            lifecycleHandlers.isOnline = true
            supervisor ! Running(ctx.self)
          }.failed.foreach(throwable ⇒ ctx.self ! UnderlyingHookFailed(throwable))
          Behaviors.same
      }

      /*
       * Defines action for messages which can be received in [[ComponentLifecycleState.Running]] state
       *
       * @param runningMessage message representing a message received in [[ComponentLifecycleState.Running]] state
       */
      val onRun: Behavior[TopLevelActorMessage] = Behaviors.receiveMessagePartial {
        case Lifecycle(message) ⇒
          onLifecycle(handlersWrapper, message)
          Behaviors.same
        case x: CommandMessage ⇒
          onRunningCompCommandMessage(handlersWrapper, cswCtx.commandResponseManager, x)
          Behaviors.same
        case msg ⇒
          log.error(s"Component TLA cannot handle message :[$msg]")
          Behaviors.same
      }

      /*
       * Defines processing for a [[TopLevelActorMessage]] received by the actor instance.
       *
       * @param msg componentMessage received from supervisor
       * @return the existing behavior
       */
      Behaviors
        .receiveMessage[TopLevelActorMessage] {
          msg ⇒
            log.debug(s"Component TLA in lifecycle state :[$lifecycleState] received message :[$msg]")
            (lifecycleState, msg) match {
              case (_, msg: TopLevelActorCommonMessage)                          ⇒ onCommon
              case (ComponentLifecycleState.Idle, msg: TopLevelActorIdleMessage) ⇒ onIdle
              case (ComponentLifecycleState.Running, msg: RunningMessage)        ⇒ onRun
              case _ ⇒
                log.error(s"Unexpected message :[$msg] received by component in lifecycle state :[$lifecycleState]")
                Behaviors.same
            }
        }
        .receiveSignal {
          case (_, PostStop) ⇒
            onPostStop(handlersWrapper)
            Behaviors.same
        }
    })

  def handleValidate(handlersWrapper: HandlersWrapper,
                     commandMessage: CommandMessage,
                     replyTo: ActorRef[ValidateResponse]): Unit = {
    val validationResponse = handlersWrapper.validateCommand(commandMessage)
    replyTo ! validationResponse.asInstanceOf[ValidateResponse]
  }

  def handleOneway(handlersWrapper: HandlersWrapper, commandMessage: CommandMessage, replyTo: ActorRef[OnewayResponse]): Unit = {
    val validationResponse = handlersWrapper.validateCommand(commandMessage)
    replyTo ! validationResponse.asInstanceOf[OnewayResponse]

    validationResponse match {
      case Accepted(_) ⇒ handlersWrapper.oneway(commandMessage)
      case invalid: Invalid ⇒
        handlersWrapper.log.debug(s"Command not forwarded to TLA post validation. ValidationResponse was [$invalid]")
    }
  }

  def handleSubmit(handlersWrapper: HandlersWrapper,
                   commandResponseManager: CommandResponseManager,
                   commandMessage: CommandMessage,
                   replyTo: ActorRef[SubmitResponse]): Unit = {
    handlersWrapper.validateCommand(commandMessage) match {
      case Accepted(runId) =>
        commandResponseManager.commandResponseManagerActor ! AddOrUpdateCommand(Started(runId))
        val submitResponse = handlersWrapper.submit(commandMessage)
        // The response is used to update the CRM, it may still be `Started` if is a long running command
        commandResponseManager.commandResponseManagerActor ! AddOrUpdateCommand(submitResponse)

        replyTo ! submitResponse
      case invalid: Invalid =>
        handlersWrapper.log.debug(s"Command not forwarded to TLA post validation. ValidationResponse was [$invalid]")
        replyTo ! invalid
    }
  }
  /*
   * Defines processing for a [[akka.actor.typed.PostStop]] Signal received by the actor instance
   */
  def onPostStop(handlersWrapper: HandlersWrapper): Unit = {
    val shutdownTimeout: FiniteDuration = 10.seconds
    handlersWrapper.log.warn("Component TLA is shutting down")
    try {
      Await.result(handlersWrapper.shutdown(), shutdownTimeout)
    } catch {
      case NonFatal(throwable) ⇒ handlersWrapper.log.error(throwable.getMessage, ex = throwable)
    }
  }

  /*
   * Defines action for messages which alter the [[ComponentLifecycleState]] state
   *
   * @param toComponentLifecycleMessage message representing a lifecycle message sent by the supervisor to the component
   */
  def onLifecycle(handlersWrapper: HandlersWrapper, toComponentLifecycleMessage: ToComponentLifecycleMessage): Unit =
    toComponentLifecycleMessage match {
      case GoOnline  ⇒ handlersWrapper.goOnline()
      case GoOffline ⇒ handlersWrapper.goOffline()
    }

  /*
   * Defines action for messages which represent a [[csw.params.commands.Command]]
   *
   * @param commandMessage message encapsulating a [[csw.params.commands.Command]]
   */
  def onRunningCompCommandMessage(handlersWrapper: HandlersWrapper,
                                  commandResponseManager: CommandResponseManager,
                                  commandMessage: CommandMessage): Unit = commandMessage match {
    case Validate(_, replyTo) ⇒ handleValidate(handlersWrapper, commandMessage, replyTo)
    case Oneway(_, replyTo)   ⇒ handleOneway(handlersWrapper, commandMessage, replyTo)
    case Submit(_, replyTo)   ⇒ handleSubmit(handlersWrapper, commandResponseManager, commandMessage, replyTo)
  }
}
