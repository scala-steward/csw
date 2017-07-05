package csw.framework.common

/**
 * Messages sent to components to notify of lifecycle changes
 */
sealed trait ToComponentLifecycleMessage

object ToComponentLifecycleMessage {

  /**
   * Component has been requested to prepare itself to be shutdown
   */
  case object DoShutdown extends ToComponentLifecycleMessage

  /**
   * The component has been requested to restart by re-executing its initialization process.
   */
  case object DoRestart extends ToComponentLifecycleMessage

  /**
   * Supervisor reports to the component that it is Running and Online
   */
  case object Running extends ToComponentLifecycleMessage

  /**
   * Supervsior reports that the component is Running but is Offline
   */
  case object RunningOffline extends ToComponentLifecycleMessage

  // Report to component that a lifecycle failure has occurred for logging, etc.
  /**
   * Message sent by the Supervisor to the component when it has entered a lifecycle failure state
   * The component can take action when receiving this message such as logging
   *
   * @param state the state that has failed
   * @param reason a string describing the reason for the failure
   */
  case class LifecycleFailureInfo(state: LifecycleState, reason: String) extends ToComponentLifecycleMessage

}

/**
 * Messages from component indicating events
 */
sealed trait FromComponentLifecycleMessage

object FromComponentLifecycleMessage {
  // Component indicates it has Initialized successfully
  case object Initialized extends FromComponentLifecycleMessage

  /**
   * Component indicates it failed to initialize with the given reason
   *
   * @param reason the reason for failing to initialize as a String
   */
  case class InitializeFailure(reason: String) extends FromComponentLifecycleMessage

  /**
   * Component indicates it has completed shutting down successfully
   */
  case object ShutdownComplete extends FromComponentLifecycleMessage with ToComponentLifecycleMessage

  /**
   * Component indicates it has failed to shutdown properly with the given reason
   *
   * @param reason reason for failing to shutdown as a String
   */
  case class ShutdownFailure(reason: String) extends FromComponentLifecycleMessage

  /**
   * Diagnostic message to shutdown and then exit supervisor/component
   */
  case object HaltComponent extends FromComponentLifecycleMessage

}
