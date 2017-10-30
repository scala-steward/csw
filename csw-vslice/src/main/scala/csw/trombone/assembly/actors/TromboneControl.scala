package csw.trombone.assembly.actors

import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.messages.CommandMessage.Submit
import csw.messages.params.models.RunId
import csw.trombone.assembly.TromboneControlMsg.{GoToStagePosition, UpdateTromboneHcd}
import csw.trombone.assembly.{Algorithms, AssemblyContext, TromboneControlMsg}
import csw.trombone.hcd.TromboneHcdState

object TromboneControl {
  def behavior(ac: AssemblyContext, tromboneHcd: Option[ActorRef[Submit]]): Behavior[TromboneControlMsg] =
    Actor.immutable { (ctx, msg) ⇒
      msg match {
        case GoToStagePosition(stagePosition) =>
          assert(stagePosition.units == ac.stagePositionUnits)
          val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig.get, stagePosition.head)
          assert(
            encoderPosition > ac.controlConfig.get.minEncoderLimit && encoderPosition < ac.controlConfig.get.maxEncoderLimit
          )
          tromboneHcd.foreach(
            _ ! Submit(TromboneHcdState.positionSC(RunId(), ac.obsId, encoderPosition),
                       ctx.spawnAnonymous(Actor.ignore))
          )
          Actor.same
        case UpdateTromboneHcd(runningIn) =>
          behavior(ac, runningIn)
      }
    }
}
