package csw.vslice.assembly

import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import csw.vslice.framework.RunningHcdMsg.Submit
import csw.vslice.hcd.models.TromboneHcdState

object TromboneControl {
  def behaviour(ac: AssemblyContext, tromboneHCD: Option[ActorRef[Submit]]): Behavior[TromboneControlMsg] =
    Actor.immutable { (_, msg) ⇒
      msg match {
        case GoToStagePosition(stagePosition) =>
          assert(stagePosition.units == ac.stagePositionUnits)
          val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition.head)
          assert(
            encoderPosition > ac.controlConfig.minEncoderLimit && encoderPosition < ac.controlConfig.maxEncoderLimit
          )
          tromboneHCD.foreach(_ ! Submit(TromboneHcdState.positionSC(ac.commandInfo, encoderPosition)))
          Actor.same
        case UpdateTromboneHCD(tromboneHCDIn1) =>
          behaviour(ac, tromboneHCDIn1)
      }
    }
}
