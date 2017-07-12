package csw.vslice.assembly

import akka.typed.ActorRef
import csw.param.DoubleParameter

sealed trait TromboneControlMsg
case class UpdateTromboneHCD(tromboneHCD: Option[ActorRef[Any]]) extends TromboneControlMsg
case class GoToStagePosition(stagePosition: DoubleParameter)     extends TromboneControlMsg
