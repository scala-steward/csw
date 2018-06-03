package csw.framework.internal.pubsub

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import csw.messages.framework.PubSub
import csw.messages.framework.PubSub.{Publish, Subscribe, SubscribeOnly, Unsubscribe}
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

  def ready[T](subscribers: Set[(ActorRef[T], T => Boolean)], log: Logger): Behavior[PubSub[T]] = {
    // This function is provided to pass all CurrentStatus values
    val allTrue = (_: T) => true

    Behaviors
      .receive[PubSub[T]] { (ctx, msg) =>
        msg match {
          case SubscribeOnly(ref, f) =>
            if (!subscribers.exists(_._1 == ref)) {
              ctx.watch(ref)
              ready(subscribers + Tuple2(ref, f), log)
            } else ready(subscribers, log)

          case Subscribe(ref) =>
            if (!subscribers.exists(_._1 == ref)) {
              ctx.watch(ref)
              ready(subscribers + Tuple2(ref, allTrue), log)
            } else ready(subscribers, log)

          case Unsubscribe(ref) =>
            ctx.unwatch(ref)
            ready(subscribers.filterNot(_._1 == ref), log)

          case Publish(data) =>
            log.info(s"Notifying subscribers :[${subscribers.mkString(",")}] with data :[$data]")
            subscribers.foreach(s => if (s._2(data)) s._1 ! data)
            ready(subscribers, log)
        }
      }
      .receiveSignal {
        case (_, Terminated(ref)) =>
          log.debug(s"Pubsub received terminated for: $ref")
          ready(subscribers.filterNot(_._1 == ref), log)
      }
  }

}
