//package csw.common.framework.generalizingcomponents
//
//import akka.typed.ActorRef
//import csw.common.ccs.CommandStatus.CommandResponse
//import csw.param.Parameters.{ControlCommand, Setup}
//import csw.trombone.assembly.actors.TromboneStateActor.StateWasSet
//
//sealed trait LifecycleStateNew
//
//object LifecycleStateNew {
//  case object LifecycleWaitingForInitialized extends LifecycleStateNew
//  case object LifecycleInitializeFailure     extends LifecycleStateNew
//  case object LifecycleRunning               extends LifecycleStateNew
//  case object LifecycleRunningOffline        extends LifecycleStateNew
//  case object LifecyclePreparingToShutdown   extends LifecycleStateNew
//  case object LifecycleShutdown              extends LifecycleStateNew
//  case object LifecycleShutdownFailure       extends LifecycleStateNew
//}
//
/////////////////
//
//sealed trait ToComponentLifecycleMessageNew
//object ToComponentLifecycleMessageNew {
//  case object Shutdown                                                      extends ToComponentLifecycleMessageNew
//  case object Restart                                                       extends ToComponentLifecycleMessageNew
//  case object GoOffline                                                     extends ToComponentLifecycleMessageNew
//  case object GoOnline                                                      extends ToComponentLifecycleMessageNew
//  case class LifecycleFailureInfo(state: LifecycleStateNew, reason: String) extends ToComponentLifecycleMessageNew
//}
//
/////////////////
//
//sealed trait ComponentResponseMode
//object ComponentResponseMode {
//  case object Idle                                                    extends ComponentResponseMode
//  case class Initialized(componentRef: ActorRef[InitialComponentMsg]) extends ComponentResponseMode
//  case class Running(componentRef: ActorRef[RunningComponentMsg])     extends ComponentResponseMode
//}
//
/////////////////
//
//sealed trait FromComponentLifecycleMessageNew extends ComponentResponseMode
//object FromComponentLifecycleMessageNew {
//  case class InitializeFailure(reason: String) extends FromComponentLifecycleMessageNew
//  case class ShutdownFailure(reason: String)   extends FromComponentLifecycleMessageNew
//  case object HaltComponent                    extends FromComponentLifecycleMessageNew
//  case object ShutdownComplete                 extends FromComponentLifecycleMessageNew
//}
//
///////////////
//
//sealed trait PubSub[T]
//
//object PubSub {
//  case class Subscribe[T](ref: ActorRef[T])   extends PubSub[T]
//  case class Unsubscribe[T](ref: ActorRef[T]) extends PubSub[T]
//  case class Publish[T](data: T)              extends PubSub[T]
//}
//
/////////////////
//
//sealed trait CommandMsgs
//object CommandMsgs {
//  case class CommandStart(replyTo: ActorRef[CommandResponse]) extends CommandMsgs
//  case object StopCurrentCommand                              extends CommandMsgs
//  case class SetStateResponseE(response: StateWasSet)         extends CommandMsgs
//}
//
/////////////////
//
//trait DomainMsgNew
//
/////////////////
//
//sealed trait ComponentMsg extends HcdMsgNew with AssemblyMsgNew
//
/////////////////
//
//sealed trait IdleComponentMsg extends ComponentMsg
//object IdleComponentMsg {
//  case object Initialize extends IdleComponentMsg
//  case object Start      extends IdleComponentMsg
//}
//
/////////////////
//
//sealed trait InitialComponentMsg extends ComponentMsg
//object InitialComponentMsg {
//  case object Run extends InitialComponentMsg
//}
//
/////////////////
//
//sealed trait RunningComponentMsg extends ComponentMsg
//object RunningComponentMsg {
//  case class Lifecycle(message: ToComponentLifecycleMessageNew) extends RunningComponentMsg
//  case class DomainComponentMsg[T <: DomainMsgNew](message: T)  extends RunningComponentMsg
//}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//sealed trait RunMsg
//
//sealed trait HcdMsgNew
//sealed trait HcdRunMsg extends HcdMsgNew with RunMsg
//object HcdMsgNew {
//  case class Submit(command: Setup) extends HcdRunMsg
//}
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//sealed trait AssemblyMsgNew
//
//sealed trait AssemblyRunMsg extends AssemblyMsgNew with RunMsg
//object AssemblyMsgNew {
//  case class Submit(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends AssemblyRunMsg
//  case class Oneway(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends AssemblyRunMsg
//}
//
/////////////////////////
