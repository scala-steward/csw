package csw.framework.internal.component

import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior, PostStop, Signal}
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.CommonMessage.{TrackingEventReceived, UnderlyingHookFailed}
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages.IdleMessage.Initialize
import csw.messages.RunningMessage.{DomainMessage, Lifecycle}
import csw.messages.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.messages._
import csw.messages.framework.ComponentInfo
import csw.messages.framework.LocationServiceUsage.RegisterAndTrackServices
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.ComponentLogger

import scala.async.Async.{async, await}
import scala.concurrent.Await
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.reflect.ClassTag
import scala.util.control.NonFatal

/**
 * The Behavior of a component actor represented as a mutable behavior
 * @param ctx                  The Actor Context under which the actor instance of this behavior is created
 * @param componentInfo        ComponentInfo as described in the configuration file
 * @param supervisor           The actor reference of the supervisor actor which created this component
 * @param lifecycleHandlers    The implementation of handlers which defines the domain actions to be performed by this
 *                             component
 * @param locationService      The single instance of Location service created for a running application
 * @tparam Msg                 The type of messages created for domain specific message hierarchy of any component
 */
class ComponentBehavior[Msg <: DomainMessage: ClassTag](
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    supervisor: ActorRef[FromComponentLifecycleMessage],
    lifecycleHandlers: ComponentHandlers[Msg],
    locationService: LocationService
) extends ComponentLogger.MutableActor[ComponentMessage](ctx, componentInfo.name) {

  import ctx.executionContext

  val shutdownTimeout: FiniteDuration = 10.seconds

  var lifecycleState: ComponentLifecycleState = ComponentLifecycleState.Idle

  var componentHandlers: ComponentHandlers[Msg] = lifecycleHandlers

  ctx.self ! Initialize

  /**
   * Defines processing for a [[csw.messages.ComponentMessage]] received by the actor instance.
   * @param msg      ComponentMessage received from supervisor
   * @return         The same behavior
   */
  def onMessage(msg: ComponentMessage): Behavior[ComponentMessage] = {
    log.debug(s"Component TLA in lifecycle state :[$lifecycleState] received message :[$msg]")
    (lifecycleState, msg) match {
      case (_, msg: CommonMessage)                                ⇒ onCommon(msg)
      case (ComponentLifecycleState.Idle, msg: IdleMessage)       ⇒ onIdle(msg)
      case (ComponentLifecycleState.Running, msg: RunningMessage) ⇒ onRun(msg)
      case _                                                      ⇒ log.error(s"Unexpected message :[$msg] received by component in lifecycle state :[$lifecycleState]")
    }
    this
  }

  /**
   * Defines processing for a [[akka.typed.Signal]] received by the actor instance.
   * @return        The same behvaior
   */
  override def onSignal: PartialFunction[Signal, Behavior[ComponentMessage]] = {
    case PostStop ⇒
      log.warn("Component TLA is shutting down")
      try {
        log.info("Invoking lifecycle handler's onShutdown hook")
        Await.result(componentHandlers.onShutdown(), shutdownTimeout)
      } catch {
        case NonFatal(throwable) ⇒ log.error(throwable.getMessage, ex = throwable)
      }
      this
  }

  /**
   * Defines action for messages which can be received in any [[csw.framework.internal.component.ComponentLifecycleState]] state
   * @param commonMessage Message representing a message received in any lifecycle state
   */
  private def onCommon(commonMessage: CommonMessage): Unit = commonMessage match {
    case UnderlyingHookFailed(exception) ⇒
      log.error(exception.getMessage, ex = exception)
      throw exception
    case TrackingEventReceived(trackingEvent) ⇒
      componentHandlers = componentHandlers.onLocationTrackingEvent(trackingEvent)
  }

  /**
   * Defines action for messages which can be received in [[csw.framework.internal.component.ComponentLifecycleState.Idle]] state
   * @param idleMessage  Message representing a message received in [[csw.framework.internal.component.ComponentLifecycleState.Idle]] state
   */
  private def onIdle(idleMessage: IdleMessage): Unit = idleMessage match {
    case Initialize ⇒
      async {
        log.info("Invoking lifecycle handler's initialize hook")
        componentHandlers = await(componentHandlers.initialize())
        log.debug(
          s"Component TLA is changing lifecycle state from [$lifecycleState] to [${ComponentLifecycleState.Initialized}]"
        )
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
        componentHandlers.isOnline = true
        supervisor ! Running(ctx.self)
      }.failed.foreach(throwable ⇒ ctx.self ! UnderlyingHookFailed(throwable))
  }

  /**
   * Defines action for messages which can be received in [[csw.framework.internal.component.ComponentLifecycleState.Running]] state
   * @param runningMessage  Message representing a message received in [[csw.framework.internal.component.ComponentLifecycleState.Running]] state
   */
  private def onRun(runningMessage: RunningMessage): Unit = runningMessage match {
    case Lifecycle(message) ⇒ onLifecycle(message)
    case x: Msg ⇒
      log.info(s"Invoking lifecycle handler's onDomainMsg hook with msg :[$x]")
      componentHandlers = componentHandlers.onDomainMsg(x)
    case x: CommandMessage ⇒ onRunningCompCommandMessage(x)
    case msg               ⇒ log.error(s"Component TLA cannot handle message :[$msg]")
  }

  /**
   * Defines action for messages which alter the [[csw.framework.internal.component.ComponentLifecycleState]] state
   * @param toComponentLifecycleMessage  Message representing a lifecycle message sent by the supervisor to the component
   */
  private def onLifecycle(toComponentLifecycleMessage: ToComponentLifecycleMessage): Unit =
    toComponentLifecycleMessage match {
      case GoOnline ⇒
        // process only if the component is offline currently
        if (!componentHandlers.isOnline) {
          log.info("Invoking lifecycle handler's onGoOnline hook")
          componentHandlers = componentHandlers.onGoOnline()
          componentHandlers.isOnline = true
          log.debug(s"Component TLA is Online")
        }
      case GoOffline ⇒
        // process only if the component is online currently
        if (componentHandlers.isOnline) {
          log.info("Invoking lifecycle handler's onGoOffline hook")
          componentHandlers = componentHandlers.onGoOffline()
          componentHandlers.isOnline = false
          log.debug(s"Component TLA is Offline")
        }
    }

  /**
   * Defines action for messages which represent a [[csw.messages.ccs.commands.Command]]
   * @param commandMessage  Message encapsulating a [[csw.messages.ccs.commands.Command]]
   */
  def onRunningCompCommandMessage(commandMessage: CommandMessage): Unit = {

    val validationResponse = commandMessage match {
      case _: Oneway =>
        log.info(s"Invoking lifecycle handler's onOneway hook with msg :[$commandMessage]")
        componentHandlers.onOneway(commandMessage.command)
      case _: Submit =>
        log.info(s"Invoking lifecycle handler's onSubmit hook with msg :[$commandMessage]")
        componentHandlers.onSubmit(commandMessage.command, commandMessage.replyTo)
    }
    commandMessage.replyTo ! validationResponse._2
  }
}
