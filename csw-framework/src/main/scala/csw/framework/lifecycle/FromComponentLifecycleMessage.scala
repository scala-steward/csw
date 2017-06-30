package csw.framework.lifecycle

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
  case object ShutdownComplete extends FromComponentLifecycleMessage

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
