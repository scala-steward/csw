package csw.common.utils

import akka.actor.typed.ActorRef
import csw.common.framework.LockingResponse
import csw.common.params.models.Prefix
import csw.common.scaladsl.SupervisorLockMessage.Lock

import scala.concurrent.duration.DurationLong

object LockCommandFactory {
  def make(prefix: Prefix, replyTo: ActorRef[LockingResponse]) = Lock(prefix, replyTo, 10.seconds)
}
