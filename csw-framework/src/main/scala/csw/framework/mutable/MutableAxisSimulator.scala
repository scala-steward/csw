package csw.framework.mutable

import akka.typed.scaladsl.Actor
import csw.framework.immutable.AxisConfig

object MutableAxisSimulator {
  def limitMove(ac: AxisConfig, request: Int): Int = Math.max(Math.min(request, ac.highLimit), ac.lowLimit)

  // Check to see if position is in the "limit" zones
  def isHighLimit(ac: AxisConfig, current: Int): Boolean = current >= ac.highUser

  def isLowLimit(ac: AxisConfig, current: Int): Boolean = current <= ac.lowUser

  def isHomed(ac: AxisConfig, current: Int): Boolean = current == ac.home
}

//class MutableAxisSimulator() extends Actor.MutableBehavior[] {}
