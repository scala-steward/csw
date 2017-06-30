package csw.framework.lifecycle

sealed trait LifecycleState

object LifecycleState {

  /**
   * State of the supervisor/component when started and Supervisor is waiting for the [[csw.framework.lifecycle.FromComponentLifecycleMessage.Initialized]] lifecycle
   * message from the component
   */
  case object LifecycleWaitingForInitialized extends LifecycleState

  /**
   * State of the Supervisor/component after the component has indicated it could not initialize successfully.
   * Component receives the [[csw.framework.lifecycle.ToComponentLifecycleMessage.LifecycleFailureInfo]] message and can take a failure action.
   */
  case object LifecycleInitializeFailure extends LifecycleState

  /**
   * State of the supervisor/component after Supervisor receives the [[FromComponentLifecycleMessage.Initialized]]
   * message from the component.
   * Component receives a [[csw.framework.lifecycle.ToComponentLifecycleMessage.Running]] message indicating this.
   * Component is Running and Online at this point.
   */
  case object LifecycleRunning extends LifecycleState

  /**
   * State of the supervisor/component after Supervisor receives an [[SupervisorExternalMessage.ExComponentOffline]]
   * message from an external actor to place the component offline.
   * The component receives the [[csw.framework.lifecycle.ToComponentLifecycleMessage.RunningOffline]] message indicating this change.
   */
  case object LifecycleRunningOffline extends LifecycleState

  /**
   * State of the supervisor/component after Supervisor receives an [[SupervisorExternalMessage.ExComponentShutdown]]
   * message from an external actor to shutdown the component.
   * The component receives the [[ToComponentLifecycleMessage.DoShutdown]] message indicating this and should then
   * take actions to prepare itself for shutdown.
   * The Supervisor waits for either the [[FromComponentLifecycleMessage.ShutdownComplete]] or [[FromComponentLifecycleMessage.ShutdownFailure]]
   * message from the component.
   */
  case object LifecyclePreparingToShutdown extends LifecycleState

  /**
   * State of the Supervisor/component after the component has indicated it is ready to shutdown by sending the
   * the [[FromComponentLifecycleMessage.ShutdownComplete]] message to the Supervisor.
   */
  case object LifecycleShutdown extends LifecycleState

  /**
   * State of the Supervisor/component when the component indicated it could not get ready to shutdown or failed
   * to notify the Supervisor with the [[FromComponentLifecycleMessage.ShutdownComplete]] message within the
   * timeout period.
   */
  case object LifecycleShutdownFailure extends LifecycleState

}
