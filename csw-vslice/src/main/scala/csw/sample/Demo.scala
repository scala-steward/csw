package csw.sample

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.sample.Demo.Command.InitialCommand.{Log, Run}
import csw.sample.Demo.Command.RunningCommand.{Add, Multiply}
import csw.sample.Demo.{Command, Mode, Response}
import csw.sample.Demo.Command.{InitialCommand, RunningCommand, Stop}
import csw.sample.Demo.Response.{Initialized, Running}

import async.Async._
import scala.concurrent.Future

object Demo {

  sealed trait Command
  object Command {
    sealed trait InitialCommand extends Command
    object InitialCommand {
      case object Log extends InitialCommand
      case object Run extends InitialCommand
    }

    sealed trait RunningCommand extends Command
    object RunningCommand {
      case class Multiply(x: Int) extends RunningCommand
      case class Add(x: Int)      extends RunningCommand
    }

    case object Stop extends InitialCommand with RunningCommand
  }

  sealed trait Response
  object Response {
    case class Initialized(ref: ActorRef[InitialCommand]) extends Response
    case class Running(ref: ActorRef[RunningCommand])     extends Response
  }

  sealed trait Mode
  object Mode {
    case object Initial extends Mode
    case object Running extends Mode
  }

  def behvaiour(monitor: ActorRef[Response]): Behavior[Nothing] =
    Actor.mutable[Command](ctx ⇒ new Demo(ctx, monitor)).narrow
}

class Demo(ctx: ActorContext[Command], monitor: ActorRef[Response]) extends MutableBehavior[Command] {

  var mode: Mode = _
  var sum: Int   = 0

  import ctx.executionContext

  async {
    sum += await(getCurrentState)
    monitor ! Initialized(ctx.self)
    mode = Mode.Initial
  }

  override def onMessage(msg: Command): Behavior[Command] = {
    (mode, msg) match {
      case (Mode.Initial, x: InitialCommand) ⇒ onInitialCommand(x)
      case (Mode.Running, x: RunningCommand) ⇒ onRunningCommand(x)
      case _                                 ⇒ println(s"msg=$msg can be handled in mode=$mode")
    }
    this
  }

  def onInitialCommand(initialCommand: InitialCommand): Unit = initialCommand match {
    case Log  => println(sum)
    case Run  => monitor ! Running(ctx.self)
      mode = Mode.Running
    case Stop ⇒ ctx.stop(ctx.self)
  }

  def onRunningCommand(runningCommand: RunningCommand): Unit = runningCommand match {
    case Multiply(x) => sum += x
    case Add(x)      => sum *= x
    case Stop        ⇒ ctx.stop(ctx.self)
  }

  def getCurrentState: Future[Int] = ???
}
