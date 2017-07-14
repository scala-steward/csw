package csw.vslice.assembly

import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import csw.param.DoubleParameter
import csw.vslice.assembly.TromboneControl.TromboneControlMsg.{GoToStagePosition, UpdateTromboneHcd}
import csw.vslice.framework.FromComponentLifecycleMessage.Running
import csw.vslice.framework.RunningHcdMsg.Submit
import csw.vslice.hcd.models.TromboneHcdState

object TromboneControl {
  def behaviour(ac: AssemblyContext, running: Option[Running]): Behavior[TromboneControlMsg] =
    Actor.immutable { (_, msg) ⇒
      msg match {
        case GoToStagePosition(stagePosition) =>
          assert(stagePosition.units == ac.stagePositionUnits)
          val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition.head)
          assert(
            encoderPosition > ac.controlConfig.minEncoderLimit && encoderPosition < ac.controlConfig.maxEncoderLimit
          )
          running.foreach(_.hcdRef ! Submit(TromboneHcdState.positionSC(ac.commandInfo, encoderPosition)))
          Actor.same
        case UpdateTromboneHcd(runningIn) =>
          behaviour(ac, runningIn)
      }
    }

  sealed trait TromboneControlMsg
  object TromboneControlMsg {
    case class UpdateTromboneHcd(running: Option[Running])       extends TromboneControlMsg
    case class GoToStagePosition(stagePosition: DoubleParameter) extends TromboneControlMsg
  }
}
