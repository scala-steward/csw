package csw.vslice.assembly

import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.vslice.assembly.TromboneControlMsg.{GoToStagePosition, UpdateTromboneHcd}
import csw.vslice.framework.RunningHcdMsg.Submit
import csw.vslice.hcd.models.TromboneHcdState

object TromboneControl {
  def behaviour(ac: AssemblyContext, tromboneHcd: Option[ActorRef[Submit]]): Behavior[TromboneControlMsg] =
    Actor.immutable { (_, msg) ⇒
      msg match {
        case GoToStagePosition(stagePosition) =>
          assert(stagePosition.units == ac.stagePositionUnits)
          val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition.head)
          assert(
            encoderPosition > ac.controlConfig.minEncoderLimit && encoderPosition < ac.controlConfig.maxEncoderLimit
          )
          tromboneHcd.foreach(_ ! Submit(TromboneHcdState.positionSC(ac.commandInfo, encoderPosition)))
          Actor.same
        case UpdateTromboneHcd(runningIn) =>
          behaviour(ac, runningIn)
      }
    }
}
