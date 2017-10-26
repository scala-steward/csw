package csw.trombone.assembly.commands
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.messages._
import csw.messages.ccs.commands.Setup
import csw.trombone.assembly.FollowActorMessages.{SetZenithAngle, StopFollowing}
import csw.trombone.assembly.actors.TromboneState._
import csw.trombone.assembly._

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class SetAngleCommand(ctx: ActorContext[AssemblyCommandHandlerMsgs],
                      runId: String,
                      ac: AssemblyContext,
                      s: Setup,
                      followCommandActor: ActorRef[FollowCommandMessages],
                      tromboneHCD: Option[ActorRef[SupervisorExternalMessage]],
                      startState: TromboneState,
                      stateActor: ActorRef[PubSub[AssemblyState]])
    extends AssemblyCommand(ctx, runId, startState, stateActor) {

  implicit val timeout: Timeout = Timeout(5.seconds)

  override def startCommand(): Future[CommandExecutionResponse] = {
    publishState(TromboneState(cmdItem(cmdBusy), startState.move, startState.sodiumLayer, startState.nss))

    val zenithAngleItem = s(ac.zenithAngleKey)

    followCommandActor ! SetZenithAngle(zenithAngleItem)

    matchCompletion(Matchers.idleMatcher, tromboneHCD.get, 5.seconds) {
      case Completed(`runId`) =>
        publishState(TromboneState(cmdItem(cmdContinuous), startState.move, startState.sodiumLayer, startState.nss))
        Completed(runId)
      case Error(`runId`, message) =>
        println(s"setElevation command failed with message: $message")
        Error(runId, message)
    }
  }

  override def stopCommand(): Unit = {
    followCommandActor ! StopFollowing
  }

}
