package csw.vslice.assembly

import csw.param.DoubleParameter
import csw.vslice.framework.FromComponentLifecycleMessage.Running

sealed trait TromboneControlMsg
case class UpdateTromboneHCD(tromboneHCD: Option[Running])   extends TromboneControlMsg
case class GoToStagePosition(stagePosition: DoubleParameter) extends TromboneControlMsg
