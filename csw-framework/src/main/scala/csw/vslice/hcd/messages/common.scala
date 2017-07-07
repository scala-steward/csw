package csw.vslice.hcd.messages

import akka.typed.ActorRef

sealed trait LifecycleState

object LifecycleState {
  case object LifecycleWaitingForInitialized extends LifecycleState
  case object LifecycleInitializeFailure     extends LifecycleState
  case object LifecycleRunning               extends LifecycleState
  case object LifecycleRunningOffline        extends LifecycleState
  case object LifecyclePreparingToShutdown   extends LifecycleState
  case object LifecycleShutdown              extends LifecycleState
  case object LifecycleShutdownFailure       extends LifecycleState
}

sealed trait SupervisorExternalMessage

object SupervisorExternalMessage {
  case class SubscribeLifecycleCallback(actorRef: ActorRef[Any])   extends SupervisorExternalMessage
  case class UnsubscribeLifecycleCallback(actorRef: ActorRef[Any]) extends SupervisorExternalMessage
  case class LifecycleStateChanged(state: LifecycleState)          extends SupervisorExternalMessage
  case object ExComponentRestart                                   extends SupervisorExternalMessage
  case object ExComponentShutdown                                  extends SupervisorExternalMessage
  case object ExComponentOnline                                    extends SupervisorExternalMessage
  case object ExComponentOffline                                   extends SupervisorExternalMessage
}

sealed trait ToComponentLifecycleMessage

object ToComponentLifecycleMessage {
  case object DoShutdown                                                 extends ToComponentLifecycleMessage
  case object DoRestart                                                  extends ToComponentLifecycleMessage
  case object Running                                                    extends ToComponentLifecycleMessage
  case object RunningOffline                                             extends ToComponentLifecycleMessage
  case class LifecycleFailureInfo(state: LifecycleState, reason: String) extends ToComponentLifecycleMessage
}

sealed trait FromComponentLifecycleMessage

object FromComponentLifecycleMessage {
  case class Initialized(address: ActorRef[Initial]) extends FromComponentLifecycleMessage
  case class InitializeFailure(reason: String)       extends FromComponentLifecycleMessage
  case object ShutdownComplete                       extends FromComponentLifecycleMessage with ToComponentLifecycleMessage
  case class ShutdownFailure(reason: String)         extends FromComponentLifecycleMessage
  case object HaltComponent                          extends FromComponentLifecycleMessage
}
