package csw.vslice.ccs

import akka.typed.ActorRef
import csw.param.StateVariable.CurrentState
import csw.vslice.ccs.CommandStatus.CommandResponse

sealed trait MultiStateMatcherMsgs
object MultiStateMatcherMsgs {
  sealed trait WaitingMsg                                                                 extends MultiStateMatcherMsgs
  case class StartMatch(matchers: List[StateMatcher], replyTo: ActorRef[CommandResponse]) extends WaitingMsg
  private[ccs] sealed trait ExecutingMsg                                                  extends MultiStateMatcherMsgs
  private[ccs] case class StateUpdate(currentState: CurrentState)                         extends ExecutingMsg
  private[ccs] case object Stop                                                           extends ExecutingMsg
}

class AAAAAA
