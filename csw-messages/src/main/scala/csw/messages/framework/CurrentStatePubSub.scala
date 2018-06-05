package csw.messages.framework

import akka.actor.typed.ActorRef
import csw.messages.TMTSerializable
import csw.messages.params.states.{CurrentState, StateName}

/**
 * Represents the protocol or messages about publishing data and subscribing it
 */
sealed trait CurrentStatePubSub extends TMTSerializable
object CurrentStatePubSub {

  sealed trait CurrentStateSubscriberMessage extends CurrentStatePubSub

  /**
   * Represents a subscribe action
   *
   * @param ref the reference of subscriber used to notify to when some data is published
   * @tparam T represents the type of data that is subscribed
   */
  case class Subscribe(ref: ActorRef[CurrentState]) extends CurrentStateSubscriberMessage

  /**
   * Represents a subscribe action with a filter function
   *
   * @param ref the reference of subscriber used to notify to when some data is published
   * @tparam T represents the type of data that is subscribed
   */
  case class SubscribeOnly(ref: ActorRef[CurrentState], stateNames: Seq[StateName]) extends CurrentStateSubscriberMessage

  /**
   * Represents a unsubscribe action
   *
   * @param ref the reference of subscriber that no longer wishes to receive notification for published data
   * @tparam T represents the type of data that is subscribed
   */
  case class Unsubscribe(ref: ActorRef[CurrentState]) extends CurrentStateSubscriberMessage

  sealed trait CurrentStatePublisherMessage extends CurrentStatePubSub

  /**
   * Represents a publish action
   *
   * @param data of type T that gets published
   * @tparam T represents the type of data that is published
   */
  case class Publish(data: CurrentState) extends CurrentStatePublisherMessage
}
