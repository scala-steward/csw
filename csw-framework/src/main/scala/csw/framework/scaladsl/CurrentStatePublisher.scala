package csw.framework.scaladsl

import akka.actor.typed.ActorRef
import csw.common.framework.PubSub.{Publish, PublisherMessage}
import csw.common.params.states.CurrentState

/**
 * Wrapper API for publishing [[csw.common.params.states.CurrentState]] of a component
 *
 * @param publisherActor the wrapped actor
 */
class CurrentStatePublisher private[framework] (publisherActor: ActorRef[PublisherMessage[CurrentState]]) {

  /**
   * Publish [[csw.common.params.states.CurrentState]] to the subscribed components
   *
   * @param currentState [[csw.common.params.states.CurrentState]] to be published
   */
  def publish(currentState: CurrentState): Unit = publisherActor ! Publish(currentState)

}
