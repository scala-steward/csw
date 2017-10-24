package csw.framework.internal.component

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior, PostStop}
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.CommonMessage.{TrackingEventReceived, UnderlyingHookFailed}
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages.RunningMessage.{DomainMessage, Lifecycle}
import csw.messages.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.messages._
import csw.messages.framework.ComponentInfo
import csw.messages.framework.LocationServiceUsage.RegisterAndTrackServices
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.ComponentLogger

import scala.async.Async.{async, await}
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.reflect.ClassTag
import scala.util.control.NonFatal

object ComponentBehaviorImmutable {

  def componentBehaviorImmutable[Msg <: DomainMessage: ClassTag](
      context: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      supervisor: ActorRef[FromComponentLifecycleMessage],
      lifecycleHandlers: ComponentHandlers[Msg],
      locationService: LocationService,
      lifecycleState: ComponentLifecycleState = ComponentLifecycleState.Idle
  ): Behavior[ComponentMessage] = {

    val shutdownTimeout: FiniteDuration       = 10.seconds
    val log                                   = ComponentLogger.immutable(context, componentInfo.name)
    implicit val ec: ExecutionContextExecutor = context.executionContext
    Actor.immutable[ComponentMessage] { (ctx, msg) ⇒
      (lifecycleState, msg) match {
        case (_, msg: CommonMessage) ⇒
          println("on Common")
          val dd = onCommon(msg, lifecycleHandlers)
          componentBehaviorImmutable(context, componentInfo, supervisor, dd, locationService, lifecycleState)
        case (ComponentLifecycleState.Idle, msg: IdleMessage) ⇒
          println("on Idle")
          async {
            val dd = await(lifecycleHandlers.initialize())
            // track all connections in component info for location updates
            if (componentInfo.locationServiceUsage == RegisterAndTrackServices) {
              componentInfo.connections.foreach(
                connection ⇒ {
                  locationService.subscribe(connection,
                                            trackingEvent ⇒ context.self ! TrackingEventReceived(trackingEvent))
                }
              )
            }
            dd.isOnline = true
            supervisor ! Running(context.self)
            componentBehaviorImmutable(context,
                                       componentInfo,
                                       supervisor,
                                       dd,
                                       locationService,
                                       ComponentLifecycleState.Running)
          }.failed.foreach { ex ⇒
            println(ex.getMessage)
            ctx.self ! UnderlyingHookFailed(ex)
          }
          Actor.same
        case (ComponentLifecycleState.Running, msg: RunningMessage) ⇒
          println("on Run")
          val dd = onRun(msg, lifecycleHandlers)
          componentBehaviorImmutable(context, componentInfo, supervisor, dd, locationService, lifecycleState)
        case _ ⇒
          println(s"Unexpected message :[$msg] received by component in lifecycle state :[$lifecycleState]")
          Actor.same
      }
    } onSignal {
      case (ctx, PostStop) ⇒
        println("Component TLA is shutting down")
        try {
          println("Invoking lifecycle handler's onShutdown hook")
          Await.result(lifecycleHandlers.onShutdown(), shutdownTimeout)
        } catch {
//          case NonFatal(throwable) ⇒ println(throwable.getMessage, ex = throwable)
          case NonFatal(throwable) ⇒ println(throwable.getMessage)
        }
        Actor.stopped
    }
  }

  def onCommon[Msg <: DomainMessage: ClassTag](
      commonMessage: CommonMessage,
      lifecycleHandlers: ComponentHandlers[Msg]
  ): ComponentHandlers[Msg] =
    commonMessage match {
      case UnderlyingHookFailed(exception) ⇒
        throw exception
      case TrackingEventReceived(trackingEvent) ⇒
        lifecycleHandlers.onLocationTrackingEvent(trackingEvent)
    }

  def onRun[Msg <: DomainMessage: ClassTag](runningMessage: RunningMessage,
                                            lifecycleHandlers: ComponentHandlers[Msg]): ComponentHandlers[Msg] = {
    val onRunningComponentHandlers = runningMessage match {
      case Lifecycle(message) ⇒ onLifecycle(message, lifecycleHandlers)
      case x: Msg ⇒
        lifecycleHandlers.onDomainMsg(x)
      case x: CommandMessage ⇒ onRunningCompCommandMessage(x, lifecycleHandlers)
      case msg               ⇒ lifecycleHandlers
    }
    onRunningComponentHandlers
  }

  def onLifecycle[Msg <: DomainMessage: ClassTag](
      toComponentLifecycleMessage: ToComponentLifecycleMessage,
      lifecycleHandlers: ComponentHandlers[Msg]
  ): ComponentHandlers[Msg] = {
    val onLifecycleComponentHandlers = toComponentLifecycleMessage match {
      case GoOnline ⇒
        // process only if the component is offline currently
        if (!lifecycleHandlers.isOnline) {
          lifecycleHandlers.isOnline = true
          lifecycleHandlers.onGoOnline()
        } else lifecycleHandlers
      case GoOffline ⇒
        // process only if the component is online currently
        if (lifecycleHandlers.isOnline) {
          lifecycleHandlers.isOnline = false
          lifecycleHandlers.onGoOffline()
        } else lifecycleHandlers
    }
    onLifecycleComponentHandlers
  }

  def onRunningCompCommandMessage[Msg <: DomainMessage: ClassTag](
      commandMessage: CommandMessage,
      lifecycleHandlers: ComponentHandlers[Msg]
  ): ComponentHandlers[Msg] = {
    val onCommandComponentHandlers = commandMessage match {
      case _: Oneway =>
        lifecycleHandlers.onOneway(commandMessage.command)
      case _: Submit =>
        lifecycleHandlers.onSubmit(commandMessage.command, commandMessage.replyTo)
    }

    val validationCommandResult = CommandValidationResponse.validationAsCommandStatus(onCommandComponentHandlers._2)
    commandMessage.replyTo ! validationCommandResult
    onCommandComponentHandlers._1
  }
}
