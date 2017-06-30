package csw.framework.examples

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.Actor
import csw.framework.examples.TromboneEngineering.{GetAxisConfig, GetAxisStats, GetAxisUpdate, GetAxisUpdateNow}
import csw.framework.lifecycle.ToComponentLifecycleMessage
import csw.framework.lifecycle.ToComponentLifecycleMessage._
import csw.param.Parameters.Setup
import csw.param.PublisherActorMessage
import csw.param.StateVariable.CurrentState

sealed trait HcdMessages[+T]

sealed trait HcdInitialMessages extends HcdMessages[Nothing]
object HcdInitialMessages {
  case class PubSub(publisherActorMessage: PublisherActorMessage) extends HcdInitialMessages
  case class Run(replyTo: ActorRef[Running])                      extends HcdInitialMessages
  case object ShutdownComplete                                    extends HcdInitialMessages

  case class Running(ref: ActorRef[HcdRunningMessages[TromboneEngineering]])
}

sealed trait HcdRunningMessages[+T]      extends HcdMessages[T]
case class DomainSpecific[T](message: T) extends HcdRunningMessages[T]

sealed trait HcdRunningSignals extends HcdRunningMessages[Nothing]
object HcdRunningSignals {
  case class Submit(setup: Setup)                      extends HcdRunningSignals
  case class CurrentStateW(currentState: CurrentState) extends HcdRunningSignals
  case class ToComponentLifecycleMessageW(toComponentLifecycleMessage: ToComponentLifecycleMessage)
      extends HcdRunningSignals
  case object ShutdownComplete extends HcdRunningSignals
}

sealed trait TromboneEngineering
object TromboneEngineering {
  case object GetAxisStats     extends TromboneEngineering
  case object GetAxisUpdate    extends TromboneEngineering
  case object GetAxisUpdateNow extends TromboneEngineering
  case object GetAxisConfig    extends TromboneEngineering
}

object A {

  def makeTla(counter: Int): Behavior[HcdInitialMessages] = Actor.immutable[HcdInitialMessages] { (ctx, msg) ⇒
    import HcdInitialMessages._
    msg match {
      case PubSub(publisherActorMessage) => println(msg); makeTla(counter + 1)
      case ShutdownComplete              => println(msg); Actor.stopped
      case Run(replyTo) =>
        println(msg)
        val value = ctx.spawnAnonymous(registeredTla(counter))
        replyTo ! Running(value)
        makeTla(counter + 1)
    }
  }

  def registeredTla(counter: Int): Behavior[HcdRunningMessages[TromboneEngineering]] =
    Actor.immutable[HcdRunningMessages[TromboneEngineering]] { (ctx, msg) ⇒
      import HcdRunningSignals._

      msg match {
        case x: HcdRunningSignals =>
          x match {
            case Submit(setup)               ⇒ println(x); registeredTla(counter + 1)
            case CurrentStateW(currentState) ⇒ println(x); registeredTla(counter + 1)
            case ToComponentLifecycleMessageW(toComponentLifecycleMessage) ⇒
              toComponentLifecycleMessage match {
                case DoShutdown                          => println(x); registeredTla(counter + 1)
                case DoRestart                           => println(x); registeredTla(counter + 1)
                case Running                             => println(x); registeredTla(counter + 1)
                case RunningOffline                      => println(x); registeredTla(counter + 1)
                case LifecycleFailureInfo(state, reason) => println(x); registeredTla(counter + 1)
              }
            case ShutdownComplete ⇒ println(x); Actor.same
          }
        case DomainSpecific(message) =>
          message match {
            case GetAxisStats     => println(message); registeredTla(counter + 1)
            case GetAxisUpdate    => println(message); registeredTla(counter + 1)
            case GetAxisUpdateNow => println(message); registeredTla(counter + 1)
            case GetAxisConfig    => println(message); registeredTla(counter + 1)
          }
      }
    }
}
