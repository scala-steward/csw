package csw.services.commons.componentlogger

import akka.typed.Behavior
import akka.typed.scaladsl.ActorContext
import csw.messages.framework.ComponentInfo
import csw.services.commons.ComponentDomainMessage
import csw.services.logging.scaladsl.FrameworkLogger

//#component-logger-mutable-actor
class MutableActorSample(ctx: ActorContext[ComponentDomainMessage], componentInfo: ComponentInfo)
    extends FrameworkLogger.MutableActor(ctx, componentInfo) {

  override def onMessage(msg: ComponentDomainMessage): Behavior[ComponentDomainMessage] = ???

}
//#component-logger-mutable-actor
