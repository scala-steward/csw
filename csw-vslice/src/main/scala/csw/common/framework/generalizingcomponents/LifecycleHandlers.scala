package csw.common.framework.generalizingcomponents

import akka.typed.scaladsl.ActorContext
import csw.common.framework.models._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

abstract class LifecycleHandlers[Msg <: DomainMsg: ClassTag](ctx: ActorContext[ComponentMsg]) {

  implicit val ec: ExecutionContext = ctx.executionContext

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
