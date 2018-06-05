package csw.framework.internal.pubsub

import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import csw.messages.framework.CurrentStatePubSub.{Publish, Subscribe, SubscribeOnly, Unsubscribe}
import csw.messages.framework.CurrentStatePubSub
import csw.messages.params.states.{CurrentState, StateName}
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

object CurrentStatePubSubBehavior {

  /**
   * Represents the protocol or messages about publishing data and subscribing it
   *
   * @param loggerFactory is a CSW LoggerFactory
   */
  def behavior(loggerFactory: LoggerFactory): Behavior[CurrentStatePubSub] = {
    Behaviors.setup[CurrentStatePubSub] { ctx =>
      val log: Logger = loggerFactory.getLogger(ctx)
      ready(Set.empty, log)
    }
  }

  def ready(subscribers: Set[(ActorRef[CurrentState], Seq[StateName])], log: Logger): Behavior[CurrentStatePubSub] =
    Behaviors
      .receive[CurrentStatePubSub] { (ctx, msg) =>
        msg match {
          case SubscribeOnly(ref, stateNames) =>
            if (!subscribers.exists(_._1 == ref)) {
              ctx.watch(ref)
              ready(subscribers + Tuple2(ref, stateNames), log)
            } else ready(subscribers, log)

          case Subscribe(ref) =>
            if (!subscribers.exists(_._1 == ref)) {
              ctx.watch(ref)
              ready(subscribers + Tuple2(ref, Seq.empty), log)
            } else ready(subscribers, log)

          case Unsubscribe(ref) =>
            ctx.unwatch(ref)
            ready(subscribers.filterNot(_._1 == ref), log)

          case Publish(data) =>
            subscribers.foreach { s =>
              if (s._2.isEmpty) s._1 ! data
              else for (name <- s._2 if name == data.stateName) s._1 ! data
            }
            ready(subscribers, log)
        }
      }
      .receiveSignal {
        case (_, Terminated(ref)) =>
          log.debug(s"Pubsub received terminated for: $ref")
          ready(subscribers.filterNot(_._1 == ref), log)
      }

}
