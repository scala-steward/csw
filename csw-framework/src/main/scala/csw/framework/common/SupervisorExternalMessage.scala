package csw.framework.common

import akka.actor.ActorRef

/**
 * Base trait of supervisor actor messages
 */
sealed trait SupervisorExternalMessage

object SupervisorExternalMessage {

  /**
   * Message used to subscribe the sender to changes in lifecycle states
   *
   * @param actorRef the actor subscribing to callbacks
   */
  case class SubscribeLifecycleCallback(actorRef: ActorRef) extends SupervisorExternalMessage

  /**
   * Message to unsubscribe  from lifecycle state changes
   */
  case class UnsubscribeLifecycleCallback(actorRef: ActorRef) extends SupervisorExternalMessage

  /**
   * Message sent to subscribers of lifecycle states
   *
   * @param state the current state as a String
   */
  case class LifecycleStateChanged(state: LifecycleState) extends SupervisorExternalMessage

  /**
   * External request to restart component initialization -- only possible in LifecycleRunning and LifecycleRunningOffline
   */
  case object ExComponentRestart extends SupervisorExternalMessage

  /**
   * External request to shutdown component -- only possible in in LifecycleRunning and LifecycleRunningOffline
   */
  case object ExComponentShutdown extends SupervisorExternalMessage

  /**
   * External request to put component onlne -- only possible in LifecycleRunningOffline
   */
  case object ExComponentOnline extends SupervisorExternalMessage

  /**
   * External request to put component offline -- only possible in LifecycleRunning
   */
  case object ExComponentOffline extends SupervisorExternalMessage

}
