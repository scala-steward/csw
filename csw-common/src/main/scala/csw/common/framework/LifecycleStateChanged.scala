package csw.common.framework

import akka.actor.typed.ActorRef
import csw.common.TMTSerializable
import csw.common.scaladsl.ComponentMessage

/**
 * LifecycleStateChanged represents a notification of state change in a component
 *
 * @param publisher the reference of component's supervisor for which the state changed
 * @param state the new state the component went into
 */
case class LifecycleStateChanged private[common] (publisher: ActorRef[ComponentMessage], state: SupervisorLifecycleState)
    extends TMTSerializable
