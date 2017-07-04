package csw.framework.examples

import akka.typed.{ActorRef, ActorSystem, Behavior}
import akka.typed.scaladsl.Actor
import csw.framework.lifecycle.ToComponentLifecycleMessage
import csw.param.Parameters.Setup
import csw.param.PublisherActorMessage
import csw.param.StateVariable.CurrentState

sealed trait InitialMessage[+T]
object InitialMessage {
  case class Run[T](replyTo: ActorRef[Running[T]]) extends InitialMessage[T]
  case object Shutdown                             extends InitialMessage[Nothing]

  case class Running[T](ref: ActorRef[RunningMessage[T]])
}

sealed trait RunningMessage[+T]
case class DomainSpecific[T](msg: T)                       extends RunningMessage[T]
case class Lifecycle(message: ToComponentLifecycleMessage) extends RunningMessage[Nothing]

abstract class HcdBehaviour[Msg, State] {

  def makeTla(state: State): Behavior[InitialMessage[Msg]] = Actor.immutable[InitialMessage[Msg]] { (ctx, msg) ⇒
    import InitialMessage._
    msg match {
      case Shutdown =>
        println(msg)
        Actor.stopped
      case Run(replyTo) =>
        println(msg)
        replyTo ! Running(ctx.spawnAnonymous(makeRunningTla(state)))
        Actor.stopped
    }
  }

  protected def makeRunningTla(state: State): Behavior[RunningMessage[Msg]]
}
////////////
sealed trait TromboneEngineering
object TromboneEngineering {
  case object GetAxisStats     extends TromboneEngineering
  case object GetAxisUpdate    extends TromboneEngineering
  case object GetAxisUpdateNow extends TromboneEngineering
  case object GetAxisConfig    extends TromboneEngineering
}

case class TromboneState(counter: Int, config: String) {
  def incr(): TromboneState = copy(counter = counter + 1)
}

object TromboneState {
  def empty = TromboneState(0, "")
}

object TromboneHcd extends HcdBehaviour[TromboneEngineering, TromboneState] {
  override def makeRunningTla(state: TromboneState): Behavior[RunningMessage[TromboneEngineering]] =
    Actor.immutable[RunningMessage[TromboneEngineering]] { (ctx, msg) ⇒
      import ToComponentLifecycleMessage._
      import TromboneEngineering._
      msg match {
        case Lifecycle(lifecycleMessage) =>
          lifecycleMessage match {
            case DoShutdown                           => println(msg); Actor.stopped
            case DoRestart                            => println(msg); makeRunningTla(TromboneState.empty)
            case Running                              => println(msg); Actor.same
            case RunningOffline                       => println(msg); makeRunningTla(state.incr())
            case LifecycleFailureInfo(state1, reason) => println(msg); makeRunningTla(state.incr())
          }
        case DomainSpecific(message) =>
          message match {
            case GetAxisStats     => println(message); makeRunningTla(state.incr())
            case GetAxisUpdate    => println(message); makeRunningTla(state.incr())
            case GetAxisUpdateNow => println(message); makeRunningTla(state.incr())
            case GetAxisConfig    => println(message); makeRunningTla(state.incr())
          }
      }
    }
}
/////////////////
object TromboneMain extends App {
  val main = Actor.deferred[Nothing] { ctx ⇒
    val ref = ctx.spawnAnonymous(TromboneHcd.makeTla(TromboneState.empty))
    ///register
    Actor.empty
  }

  ActorSystem[Nothing]("demo", main)
}
