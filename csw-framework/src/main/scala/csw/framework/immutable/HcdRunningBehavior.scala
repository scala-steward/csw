package csw.framework.immutable

import akka.typed.Behavior

abstract class HcdRunningBehavior[Msg, State] {
  def run(state: State): Behavior[Msg]
}
