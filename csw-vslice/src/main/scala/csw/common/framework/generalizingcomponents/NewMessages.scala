package csw.common.framework.generalizingcomponents

import akka.typed.ActorRef
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.framework.models._
import csw.param.Parameters.{ControlCommand, Setup}

sealed trait LifecycleStateNew

object LifecycleStateNew {
  case object LifecycleWaitingForInitialized extends LifecycleStateNew
  case object LifecycleInitializeFailure     extends LifecycleStateNew
  case object LifecycleRunning               extends LifecycleStateNew
  case object LifecycleRunningOffline        extends LifecycleStateNew
  case object LifecyclePreparingToShutdown   extends LifecycleStateNew
  case object LifecycleShutdown              extends LifecycleStateNew
  case object LifecycleShutdownFailure       extends LifecycleStateNew
}

///////////////

sealed trait ToComponentLifecycleMessageNew
object ToComponentLifecycleMessageNew {
  case object Shutdown                                                   extends ToComponentLifecycleMessageNew
  case object Restart                                                    extends ToComponentLifecycleMessageNew
  case object GoOffline                                                  extends ToComponentLifecycleMessageNew
  case object GoOnline                                                   extends ToComponentLifecycleMessageNew
  case class LifecycleFailureInfo(state: LifecycleState, reason: String) extends ToComponentLifecycleMessageNew
}

///////////////

sealed trait ComponentResponseMode
object ComponentResponseMode {
  case object Idle                                                    extends ComponentResponseMode
  case class Initialized(componentRef: ActorRef[InitialComponentMsg]) extends ComponentResponseMode
  case class Running(componentRef: ActorRef[RunningComponentMsg])     extends ComponentResponseMode
}

///////////////

sealed trait FromComponentLifecycleMessageNew extends ComponentResponseMode
object FromComponentLifecycleMessageNew {
  case class InitializeFailure(reason: String) extends FromComponentLifecycleMessageNew
  case class ShutdownFailure(reason: String)   extends FromComponentLifecycleMessageNew
  case object HaltComponent                    extends FromComponentLifecycleMessageNew
  case object ShutdownComplete                 extends FromComponentLifecycleMessageNew
}

///////////////

trait DomainMsgNew

///////////////

sealed trait ComponentMsg extends HcdMsgNew with AssemblyMsgNew

///////////////

sealed trait IdleComponentMsg extends ComponentMsg
object IdleComponentMsg {
  case object Initialize extends IdleComponentMsg
  case object Start      extends IdleComponentMsg
}

///////////////

sealed trait InitialComponentMsg extends ComponentMsg
object InitialComponentMsg {
  case object Run extends InitialComponentMsg
}

///////////////

sealed trait RunningComponentMsg extends RunningHcdMsgNew with RunningAssemblyMsgNew
object RunningComponentMsg {
  case class Lifecycle(message: ToComponentLifecycleMessageNew) extends RunningComponentMsg
  case class DomainComponentMsg[T <: DomainMsgNew](message: T)  extends RunningComponentMsg
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

sealed trait HcdMsgNew

sealed trait RunningHcdMsgNew extends HcdMsgNew
object RunningHcdMsgNew {
  case class Submit(command: Setup) extends RunningHcdMsgNew
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
sealed trait AssemblyMsgNew

sealed trait RunningAssemblyMsgNew extends AssemblyMsgNew
object RunningAssemblyMsgNew {
  case class Submit(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends RunningAssemblyMsgNew
  case class Oneway(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends RunningAssemblyMsgNew
}

///////////////////////
