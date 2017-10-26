package csw.ccs

import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior}
import csw.messages.PubSub.CommandStatePubSub
import csw.messages.PubSub.CommandStatePubSub._
import csw.messages.{CommandExecutionResponse, CommandNotAvailable, NotStarted}
import csw.services.logging.scaladsl.ComponentLogger

import scala.collection.mutable

class CommandStatePubSubBehavior(ctx: ActorContext[CommandStatePubSub], componentName: String)
    extends ComponentLogger.MutableActor[CommandStatePubSub](ctx, componentName) {

  val runIdToSubscribers: mutable.Map[String, mutable.Set[ActorRef[CommandExecutionResponse]]] = mutable.Map.empty
  val runIdToCurrentState: mutable.Map[String, CommandExecutionResponse]                       = mutable.Map.empty

  override def onMessage(msg: CommandStatePubSub): Behavior[CommandStatePubSub] = {
    msg match {
      case Add(runId)                  ⇒ add(runId)
      case Publish(runId, data)        ⇒ publish(runId, data)
      case Subscribe(runId, replyTo)   ⇒ subscribe(runId, replyTo)
      case UnSubscribe(runId, replyTo) ⇒ unSubscribe(runId, replyTo)
      case Query(runId, replyTo)       ⇒ replyTo ! runIdToCurrentState.getOrElse(runId, CommandNotAvailable)
    }
    this
  }

  private def add(runId: String) = {
    runIdToSubscribers + (runId  → Set.empty)
    runIdToCurrentState + (runId → NotStarted)
  }
  private def subscribe(runId: String, actorRef: ActorRef[CommandExecutionResponse]): Unit = {
    runIdToSubscribers
      .get(runId)
      .foreach(subscribers ⇒ runIdToSubscribers + (runId → (subscribers + actorRef)))
  }

  private def unSubscribe(runId: String, actorRef: ActorRef[CommandExecutionResponse]): Unit = {
    runIdToSubscribers
      .get(runId)
      .foreach(subscribers ⇒ runIdToSubscribers + (runId → (subscribers - actorRef)))
  }

  protected def publish(runId: String, data: CommandExecutionResponse): Unit = {
    runIdToCurrentState + (runId → data)
    log.debug(s"Notifying subscribers :[${runIdToSubscribers(runId).mkString(",")}] with data :[$data]")
    runIdToSubscribers(runId).foreach(_ ! data)
  }
}
