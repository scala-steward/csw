package csw.vslice.assembly

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.param.Parameters.Setup
import csw.param.UnitsOfMeasure.encoder
import csw.vslice.assembly.TromboneCommandMsgs.{CommandStart, SetStateResponseE, StopCurrentCommand}
import csw.vslice.assembly.TromboneStateActor.{TromboneState, TromboneStateMsg}
import csw.vslice.ccs.CommandStatus.{Completed, Error, NoLongerValid}
import csw.vslice.ccs.Validation.WrongInternalStateIssue
import csw.vslice.framework.FromComponentLifecycleMessage.Running
import csw.vslice.framework.RunningHcdMsg.Submit
import csw.vslice.hcd.models.TromboneHcdState

class SetElevationCommand(ac: AssemblyContext,
                          s: Setup,
                          tromboneHCD: Running,
                          startState: TromboneState,
                          stateActor: Option[ActorRef[TromboneStateMsg]],
                          ctx: ActorContext[TromboneCommandMsgs])
    extends MutableBehavior[TromboneCommandMsgs] {

  import TromboneCommandHandler._
  import TromboneHcdState._
  import TromboneStateActor._

  private val setStateResponseAdapter: ActorRef[StateWasSet] = ctx.spawnAdapter(SetStateResponseE)

  override def onMessage(msg: TromboneCommandMsgs): Behavior[TromboneCommandMsgs] = msg match {
    case CommandStart(replyTo) =>
      if (cmd(startState) == cmdUninitialized || (move(startState) != moveIndexed && move(startState) != moveMoving)) {
        replyTo ! NoLongerValid(
          WrongInternalStateIssue(
            s"Assembly state of ${cmd(startState)}/${move(startState)} does not allow setElevation"
          )
        )
      } else {
        val elevationItem = s(ac.naElevationKey)

        val stagePosition   = Algorithms.rangeDistanceToStagePosition(elevationItem.head)
        val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition)

        println(
          s"Using elevation as rangeDistance: ${elevationItem.head} to get stagePosition: $stagePosition to encoder: $encoderPosition"
        )

        val stateMatcher = posMatcher(encoderPosition)
        // Position key is encoder units
        val scOut = Setup(ac.commandInfo, axisMoveCK).add(positionKey -> encoderPosition withUnits encoder)
        sendState(
          SetState(cmdItem(cmdBusy),
                   moveItem(moveMoving),
                   startState.sodiumLayer,
                   startState.nss,
                   setStateResponseAdapter)
        )
        tromboneHCD.hcdRef ! Submit(scOut)

        executeMatch(ctx, stateMatcher, tromboneHCD.pubSubRef, Some(replyTo)) {
          case Completed =>
            // NOTE ---> This is the place where sodium layer state gets set to TRUE
            sendState(
              SetState(cmdItem(cmdReady),
                       moveItem(moveIndexed),
                       sodiumItem(true),
                       startState.nss,
                       setStateResponseAdapter)
            )
          case Error(message) =>
            println(s"setElevation command match failed with message: $message")
        }
      }
      this
    case StopCurrentCommand =>
      tromboneHCD.hcdRef ! Submit(cancelSC(s.info))
      this
  }

  private def sendState(setState: SetState): Unit = {
//    implicit val timeout = Timeout(5.seconds)
//    stateActor.foreach(actorRef => Await.ready(actorRef ? setState, timeout.duration))
  }
}

object SetElevationCommand {

  def make(ac: AssemblyContext,
           s: Setup,
           tromboneHCD: Running,
           startState: TromboneState,
           stateActor: Option[ActorRef[TromboneStateMsg]]): Behavior[TromboneCommandMsgs] =
    Actor.mutable(ctx ⇒ new SetElevationCommand(ac, s, tromboneHCD, startState, stateActor, ctx))
}
