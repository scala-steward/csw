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

abstract class HcdBehaviour[T] {

  def makeInitialTla(counter: Int): Behavior[InitialMessage[T]] = Actor.immutable[InitialMessage[T]] { (ctx, msg) ⇒
    import InitialMessage._
    msg match {
      case Shutdown =>
        println(msg)
        Actor.stopped
      case Run(replyTo) =>
        println(msg)
        replyTo ! Running(ctx.spawnAnonymous(makeRunningTls(counter)))
        Actor.stopped
    }
  }

  protected def makeRunningTls(counter: Int): Behavior[RunningMessage[T]]
}

sealed trait TromboneEngineering
object TromboneEngineering {
  case object GetAxisStats     extends TromboneEngineering
  case object GetAxisUpdate    extends TromboneEngineering
  case object GetAxisUpdateNow extends TromboneEngineering
  case object GetAxisConfig    extends TromboneEngineering
}

object TromboneHcd extends HcdBehaviour[TromboneEngineering] {
  override def makeRunningTls(counter: Int): Behavior[RunningMessage[TromboneEngineering]] =
    Actor.immutable[RunningMessage[TromboneEngineering]] { (ctx, msg) ⇒
      import ToComponentLifecycleMessage._
      import TromboneEngineering._
      msg match {
        case Lifecycle(lifecycleMessage) =>
          lifecycleMessage match {
            case DoShutdown                          => println(msg); Actor.stopped
            case DoRestart                           => println(msg); makeRunningTls(0)
            case Running                             => println(msg); Actor.same
            case RunningOffline                      => println(msg); makeRunningTls(counter + 1)
            case LifecycleFailureInfo(state, reason) => println(msg); makeRunningTls(counter + 1)
          }
        case DomainSpecific(message) =>
          message match {
            case GetAxisStats     => println(message); makeRunningTls(counter + 1)
            case GetAxisUpdate    => println(message); makeRunningTls(counter + 1)
            case GetAxisUpdateNow => println(message); makeRunningTls(counter + 1)
            case GetAxisConfig    => println(message); makeRunningTls(counter + 1)
          }
      }
    }
}
