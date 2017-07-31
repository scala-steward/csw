package csw.common.framework.generalizingcomponents

import csw.common.framework.models._

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class LifecycleHandlers[Msg <: DomainMsgNew: ClassTag] {
  var isOnline: Boolean = false

  def initialize(): Future[Unit]
  def onRun(): Unit
  def onDomainMsg(msg: Msg): Unit

  def onShutdown(): Unit
  def onRestart(): Unit
  def onGoOffline(): Unit
  def onGoOnline(): Unit
  def onLifecycleFailureInfo(state: LifecycleState, reason: String): Unit
}
