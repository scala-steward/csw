package csw.vslice.framework

import akka.typed.ActorRef
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState
import csw.vslice.framework.FromComponentLifecycleMessage.Running

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

///////////////

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

///////////////

sealed trait ToComponentLifecycleMessage

object ToComponentLifecycleMessage {
  case object DoShutdown                                                 extends ToComponentLifecycleMessage
  case object DoRestart                                                  extends ToComponentLifecycleMessage
  case object Running                                                    extends ToComponentLifecycleMessage
  case object RunningOffline                                             extends ToComponentLifecycleMessage
  case class LifecycleFailureInfo(state: LifecycleState, reason: String) extends ToComponentLifecycleMessage
}

///////////////

sealed trait FromComponentLifecycleMessage

object FromComponentLifecycleMessage {
  case class Initialized(hcdRef: ActorRef[InitialHcdMsg], pubSubRef: ActorRef[PubSub[CurrentState]])
      extends FromComponentLifecycleMessage
  case class Running(hcdRef: ActorRef[RunningHcdMsg], pubSubRef: ActorRef[PubSub[CurrentState]])
  case class InitializeFailure(reason: String) extends FromComponentLifecycleMessage
  case object ShutdownComplete                 extends FromComponentLifecycleMessage with ToComponentLifecycleMessage
  case class ShutdownFailure(reason: String)   extends FromComponentLifecycleMessage
  case object HaltComponent                    extends FromComponentLifecycleMessage
}

///////////////

sealed trait PubSub[T]

object PubSub {
  case class Subscribe[T](ref: ActorRef[T])   extends PubSub[T]
  case class Unsubscribe[T](ref: ActorRef[T]) extends PubSub[T]
  case class Publish[T](data: T)              extends PubSub[T]
}

///////////////

sealed trait HcdMsg

sealed trait InitialHcdMsg extends HcdMsg
object InitialHcdMsg {
  case class Run(replyTo: ActorRef[Running]) extends InitialHcdMsg
  case object ShutdownComplete               extends InitialHcdMsg with RunningHcdMsg

}

sealed trait RunningHcdMsg extends HcdMsg
object RunningHcdMsg {
  case class Lifecycle(message: ToComponentLifecycleMessage) extends RunningHcdMsg
  case class Submit(command: Setup)                          extends RunningHcdMsg
  case class DomainHcdMsg[T <: DomainMsg](msg: T)            extends RunningHcdMsg
}

///////////////

trait DomainMsg
