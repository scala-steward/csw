package csw.common.components.framework

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.TopLevelActorMessage
import csw.messages.commands.CommandIssue.OtherIssue
import csw.messages.commands.CommandResponse.{Accepted, Completed, Invalid}
import csw.messages.commands._
import csw.messages.events.{Event, EventName, SystemEvent}
import csw.services.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.api.models.{LocationRemoved, LocationUpdated, TrackingEvent}
import csw.messages.params.models.Prefix
import csw.messages.params.states.{CurrentState, StateName}
import csw.services.logging.scaladsl.Logger

import scala.concurrent.{ExecutionContext, Future}

class SampleComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswServices: CswServices)
    extends ComponentHandlers(ctx, cswServices) {
  import cswServices._

  val log: Logger                   = loggerFactory.getLogger(ctx)
  implicit val ec: ExecutionContext = ctx.executionContext

  import SampleComponentState._

  override def initialize(): Future[Unit] = {
    // DEOPSCSW-153: Accessibility of logging service to other CSW components
    log.info("Initializing Component TLA")
    Thread.sleep(100)

    //#currentStatePublisher
    // Publish the CurrentState using parameter set created using a sample Choice parameter
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    //#currentStatePublisher

    // DEOPSCSW-219: Discover component connection using HTTP protocol
    trackConnection(httpConnection)
    trackConnection(tcpConnection)
    Future.unit
  }

  override def onGoOffline(): Unit =
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(offlineChoice))))

  override def onGoOnline(): Unit =
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(onlineChoice))))

  override def onSubmit(controlCommand: ControlCommand): Unit = {
    // Adding passed in parameter to see if data is transferred properly
    commandResponseManager.addOrUpdateCommand(controlCommand.runId, Completed(controlCommand.runId))
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(submitCommandChoice))))
    processCommand(controlCommand)
  }

  override def onOneway(controlCommand: ControlCommand): Unit = {
    // Adding passed in parameter to see if data is transferred properly
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(oneWayCommandChoice))))
    processCommand(controlCommand)
  }

  // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
  private def processCommand(controlCommand: ControlCommand): Unit = {

    lazy val event = SystemEvent(Prefix("test"), EventName("system"))
    def processEvent(prefix: Prefix): Event ⇒ Unit =
      _ ⇒
        currentStatePublisher.publish(
          CurrentState(prefix, StateName("testStateName"), controlCommand.paramSet + choiceKey.set(eventReceivedChoice))
      )

    controlCommand match {
      case Setup(_, _, `setSeverityCommand`, _, _) ⇒ alarmService.setSeverity(testAlarmKey, testSeverity)

      case Setup(_, _, CommandName("publish.event.success"), _, _) ⇒ eventService.defaultPublisher.publish(event)

      case Setup(_, somePrefix, CommandName("subscribe.event.success"), _, _) ⇒
        eventService.defaultSubscriber.subscribeCallback(Set(event.eventKey), processEvent(somePrefix))

      case Setup(_, somePrefix, _, _, _) ⇒
        currentStatePublisher.publish(
          CurrentState(somePrefix, StateName("testStateName"), controlCommand.paramSet + choiceKey.set(setupConfigChoice))
        )

      case Observe(_, somePrefix, _, _, _) ⇒
        currentStatePublisher.publish(
          CurrentState(somePrefix, StateName("testStateName"), controlCommand.paramSet + choiceKey.set(observeConfigChoice))
        )
      case _ ⇒
    }
  }

  override def validateCommand(command: ControlCommand): CommandResponse = {
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(commandValidationChoice))))
    if (command.commandName.name.contains("success")) Accepted(command.runId)
    else Invalid(command.runId, OtherIssue("Testing: Received failure, will return Invalid."))
  }

  override def onShutdown(): Future[Unit] = Future {
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
    Thread.sleep(500)
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = trackingEvent match {
    case LocationUpdated(location) ⇒
      location.connection match {
        case _: AkkaConnection =>
          Future {
            Thread.sleep(100)
            currentStatePublisher.publish(
              CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(akkaLocationUpdatedChoice)))
            )
          }
        case _: HttpConnection =>
          currentStatePublisher.publish(
            CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(httpLocationUpdatedChoice)))
          )
        case _: TcpConnection =>
          currentStatePublisher.publish(
            CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(tcpLocationUpdatedChoice)))
          )
      }
    case LocationRemoved(connection) ⇒
      connection match {
        case _: AkkaConnection =>
          currentStatePublisher.publish(
            CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(akkaLocationRemovedChoice)))
          )
        case _: HttpConnection =>
          currentStatePublisher.publish(
            CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(httpLocationRemovedChoice)))
          )
        case _: TcpConnection =>
          currentStatePublisher.publish(
            CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(tcpLocationRemovedChoice)))
          )
      }
  }
}
