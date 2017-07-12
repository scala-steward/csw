package csw.vslice.assembly

import akka.typed.scaladsl.Actor
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.{ActorRef, Behavior}
import csw.vslice.framework.RunningHcdMsg.Submit
import csw.vslice.hcd.models.TromboneHcdState

class TromboneControl(ac: AssemblyContext, tromboneHCDIn: Option[ActorRef[Submit]])
    extends MutableBehavior[TromboneControlMsg] {

  var tromboneHCD: Option[ActorRef[Submit]] = tromboneHCDIn

  override def onMessage(msg: TromboneControlMsg): Behavior[TromboneControlMsg] = {
    msg match {
      case GoToStagePosition(stagePosition) =>
        assert(stagePosition.units == ac.stagePositionUnits)
        val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition.head)
        assert(
          encoderPosition > ac.controlConfig.minEncoderLimit && encoderPosition < ac.controlConfig.maxEncoderLimit
        )
        tromboneHCD.foreach(_ ! Submit(TromboneHcdState.positionSC(ac.commandInfo, encoderPosition)))

      case UpdateTromboneHCD(tromboneHCDIn1) =>
        tromboneHCD = tromboneHCDIn1
    }
    this
  }
}

object TromboneControl {
  def behaviour(assemblyContext: AssemblyContext,
                tromboneHCD: Option[ActorRef[Submit]] = None): Behavior[TromboneControlMsg] =
    Actor.mutable(ctx ⇒ new TromboneControl(assemblyContext, tromboneHCD))
}
