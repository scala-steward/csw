package csw.vslice.assembly
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.param.Parameters.Setup
import csw.vslice.assembly.TromboneCommandMsgs.{CommandStart, SetStateResponseE, StopCurrentCommand}
import csw.vslice.assembly.TromboneStateActor.{TromboneState, TromboneStateMsg}
import csw.vslice.ccs.CommandStatus.{Completed, Error, NoLongerValid}
import csw.vslice.ccs.Validation.WrongInternalStateIssue
import csw.vslice.framework.FromComponentLifecycleMessage.Running
import csw.vslice.framework.RunningHcdMsg.Submit
import csw.vslice.hcd.models.TromboneHcdState

/**
 * TMT Source Code: 10/21/16.
 */
class DatumCommand(s: Setup,
                   tromboneHCD: Running,
                   startState: TromboneState,
                   stateActor: Option[ActorRef[TromboneStateMsg]],
                   ctx: ActorContext[TromboneCommandMsgs])
    extends MutableBehavior[TromboneCommandMsgs] {

  import TromboneCommandHandler._
  import TromboneStateActor._

  private val setStateResponseAdapter: ActorRef[StateWasSet] = ctx.spawnAdapter(SetStateResponseE)

  override def onMessage(msg: TromboneCommandMsgs): Behavior[TromboneCommandMsgs] = msg match {
    case CommandStart(replyTo) =>
      if (startState.cmd.head == cmdUninitialized) {
        replyTo ! NoLongerValid(
          WrongInternalStateIssue(s"Assembly state of ${cmd(startState)}/${move(startState)} does not allow datum")
        )
      } else {
        sendState(
          SetState(cmdItem(cmdBusy),
                   moveItem(moveIndexing),
                   startState.sodiumLayer,
                   startState.nss,
                   setStateResponseAdapter)
        )
        tromboneHCD.hcdRef ! Submit(Setup(s.info, TromboneHcdState.axisDatumCK))
        TromboneCommandHandler.executeMatch(ctx, idleMatcher, tromboneHCD.pubSubRef, Some(replyTo)) {
          case Completed =>
            sendState(SetState(cmdReady, moveIndexed, sodiumLayer = false, nss = false, setStateResponseAdapter))
          case Error(message) =>
            println(s"Data command match failed with error: $message")
        }
      }
      this
    case StopCurrentCommand =>
      tromboneHCD.hcdRef ! Submit(TromboneHcdState.cancelSC(s.info))
      this

    case SetStateResponseE(response: StateWasSet) => // ignore confirmation
      this
  }

  private def sendState(setState: SetState): Unit = {
//    implicit val timeout = Timeout(5.seconds)
//    stateActor.foreach(actorRef => Await.ready(actorRef ? setState, timeout.duration))
  }
}

object DatumCommand {
  def make(s: Setup,
           tromboneHCD: Running,
           startState: TromboneState,
           stateActor: Option[ActorRef[TromboneStateMsg]]): Behavior[TromboneCommandMsgs] =
    Actor.mutable(ctx ⇒ new DatumCommand(s, tromboneHCD, startState, stateActor, ctx))
}
