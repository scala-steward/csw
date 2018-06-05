package csw.framework.scaladsl

import akka.actor.typed.ActorRef
import csw.messages.framework.CurrentStatePubSub.{CurrentStatePublisherMessage, Publish}
import csw.messages.params.states.CurrentState

/**
 * Wrapper API for publishing [[csw.messages.params.states.CurrentState]] of a component
 *
 * @param publisherActor the wrapped actor
 */
class CurrentStatePublisher private[framework] (publisherActor: ActorRef[CurrentStatePublisherMessage]) {

  /**
   * Publish [[csw.messages.params.states.CurrentState]] to the subscribed components
   *
   * @param currentState [[csw.messages.params.states.CurrentState]] to be published
   */
  def publish(currentState: CurrentState): Unit = publisherActor ! Publish(currentState)

}
