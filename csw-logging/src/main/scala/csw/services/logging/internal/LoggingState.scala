package csw.services.logging.internal

import akka.actor._

import scala.collection.mutable
import scala.concurrent.Promise

/**
 * Global state info for logging. Use with care!
 */
private[logging] object LoggingState {

//  private[logging] var loggingSys: LoggingSystem = null

  private[logging] var maybeLogActor: Option[ActorRef] = None
  @volatile private[logging] var loggerStopping        = false

  private[logging] var doTime: Boolean                   = false
  private[logging] var timeActorOption: Option[ActorRef] = None

  // Use to sync akka logging actor shutdown
  private[logging] val akkaStopPromise = Promise[Unit]
}
