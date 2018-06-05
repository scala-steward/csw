package csw.framework.internal.pubsub

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import csw.messages.framework.PubSub
import csw.messages.framework.PubSub.{Publish, Subscribe, Unsubscribe}
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

object PubSubBehavior {

  /**
   * Represents the protocol or messages about publishing data and subscribing it
   *
   * @tparam T represents the type of data that is published or subscribed
   */
  def behavior[T](loggerFactory: LoggerFactory): Behavior[PubSub[T]] = {
    Behaviors.setup[PubSub[T]] { ctx =>
      val log: Logger = loggerFactory.getLogger(ctx)
      ready(Set.empty, log)
    }
  }

  def ready[T](subscribers: Set[ActorRef[T]], log: Logger): Behavior[PubSub[T]] = {
    Behaviors
      .receive[PubSub[T]] { (ctx, msg) =>
        msg match {
          case Subscribe(ref) =>
            if (!subscribers.exists(_ == ref)) {
              ctx.watch(ref)
              ready(subscribers + ref, log)
            } else ready(subscribers, log)

          case Unsubscribe(ref) =>
            ctx.unwatch(ref)
            ready(subscribers.filterNot(_ == ref), log)

          case Publish(data) =>
            log.info(s"Notifying subscribers :[${subscribers.mkString(",")}] with data :[$data]")
            subscribers.foreach(_ ! data)
            ready(subscribers, log)
        }
      }
      .receiveSignal {
        case (_, Terminated(ref)) =>
          log.debug(s"Pubsub received terminated for: $ref")
          ready(subscribers.filterNot(_ == ref), log)
      }
  }

}
