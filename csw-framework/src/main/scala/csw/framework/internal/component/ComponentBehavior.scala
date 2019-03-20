package csw.framework.internal.component

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, PostStop}
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

/**
 * The Behavior of a component actor, represented as a mutable behavior
 *
 * @param ctx the [[akka.actor.typed.scaladsl.ActorContext]] under which the actor instance of this behavior is created
 * @param supervisor the actor reference of the supervisor actor which created this component
 * @param lifecycleHandlers the implementation of handlers which defines the domain actions to be performed by this component
 */
// scalastyle:off method.length
private[framework] object ComponentBehavior {

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
      def onCommon(commonMessage: TopLevelActorCommonMessage): Unit = commonMessage match {
        case UnderlyingHookFailed(exception) ⇒
          log.error(exception.getMessage, ex = exception)
          throw exception
        case TrackingEventReceived(trackingEvent) ⇒
          lifecycleHandlers.onLocationTrackingEvent(trackingEvent)
      }

      /*
       * Defines action for messages which can be received in [[ComponentLifecycleState.Idle]] state
       *
       * @param idleMessage message representing a message received in [[ComponentLifecycleState.Idle]] state
       */
      def onIdle(idleMessage: TopLevelActorIdleMessage): Unit = idleMessage match {
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
      }

      /*
       * Defines action for messages which can be received in [[ComponentLifecycleState.Running]] state
       *
       * @param runningMessage message representing a message received in [[ComponentLifecycleState.Running]] state
       */
      def onRun(runningMessage: RunningMessage): Unit = runningMessage match {
        case Lifecycle(message) ⇒ onLifecycle(message)
        case x: CommandMessage  ⇒ onRunningCompCommandMessage(x)
        case msg                ⇒ log.error(s"Component TLA cannot handle message :[$msg]")
      }

      /*
       * Defines action for messages which alter the [[ComponentLifecycleState]] state
       *
       * @param toComponentLifecycleMessage message representing a lifecycle message sent by the supervisor to the component
       */
      def onLifecycle(toComponentLifecycleMessage: ToComponentLifecycleMessage): Unit =
        toComponentLifecycleMessage match {
          case GoOnline  ⇒ handlersWrapper.goOnline()
          case GoOffline ⇒ handlersWrapper.goOffline()
        }

      /*
       * Defines action for messages which represent a [[csw.params.commands.Command]]
       *
       * @param commandMessage message encapsulating a [[csw.params.commands.Command]]
       */
      def onRunningCompCommandMessage(commandMessage: CommandMessage): Unit = commandMessage match {
        case Validate(_, replyTo) ⇒ handleValidate(commandMessage, replyTo)
        case Oneway(_, replyTo)   ⇒ handleOneway(commandMessage, replyTo)
        case Submit(_, replyTo)   ⇒ handleSubmit(commandMessage, replyTo)
      }

      def handleValidate(commandMessage: CommandMessage, replyTo: ActorRef[ValidateResponse]): Unit = {
        val validationResponse = handlersWrapper.validateCommand(commandMessage)
        replyTo ! validationResponse.asInstanceOf[ValidateResponse]
      }

      def handleOneway(commandMessage: CommandMessage, replyTo: ActorRef[OnewayResponse]): Unit = {
        val validationResponse = handlersWrapper.validateCommand(commandMessage)
        replyTo ! validationResponse.asInstanceOf[OnewayResponse]

        validationResponse match {
          case Accepted(_) ⇒ handlersWrapper.oneway(commandMessage)
          case invalid: Invalid ⇒
            log.debug(s"Command not forwarded to TLA post validation. ValidationResponse was [$invalid]")
        }
      }

      def handleSubmit(commandMessage: CommandMessage, replyTo: ActorRef[SubmitResponse]): Unit = {
        handlersWrapper.validateCommand(commandMessage) match {
          case Accepted(runId) =>
            commandResponseManager.commandResponseManagerActor ! AddOrUpdateCommand(Started(runId))
            val submitResponse = handlersWrapper.submit(commandMessage)
            // The response is used to update the CRM, it may still be `Started` if is a long running command
            commandResponseManager.commandResponseManagerActor ! AddOrUpdateCommand(submitResponse)

            replyTo ! submitResponse
          case invalid: Invalid =>
            log.debug(s"Command not forwarded to TLA post validation. ValidationResponse was [$invalid]")
            replyTo ! invalid
        }
      }

      /*
       * Defines processing for a [[akka.actor.typed.PostStop]] Signal received by the actor instance
       */
      def onPostStop(): Unit = {
        val shutdownTimeout: FiniteDuration = 10.seconds
        log.warn("Component TLA is shutting down")
        try {
          Await.result(handlersWrapper.shutdown(), shutdownTimeout)
        } catch {
          case NonFatal(throwable) ⇒ log.error(throwable.getMessage, ex = throwable)
        }
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
              case (_, msg: TopLevelActorCommonMessage)                          ⇒ onCommon(msg)
              case (ComponentLifecycleState.Idle, msg: TopLevelActorIdleMessage) ⇒ onIdle(msg)
              case (ComponentLifecycleState.Running, msg: RunningMessage)        ⇒ onRun(msg)
              case _ ⇒
                log.error(s"Unexpected message :[$msg] received by component in lifecycle state :[$lifecycleState]")
            }
            Behaviors.same
        }
        .receiveSignal {
          case (_, PostStop) ⇒
            onPostStop()
            Behaviors.same
        }

    })
}
