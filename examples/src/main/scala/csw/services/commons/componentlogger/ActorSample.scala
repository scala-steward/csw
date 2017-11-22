package csw.services.commons.componentlogger

import csw.messages.framework.ComponentInfo
import csw.services.logging.scaladsl.FrameworkLogger

//#component-logger-actor
class ActorSample(componentInfo: ComponentInfo) extends FrameworkLogger.Actor(componentInfo) {

  override def receive: Nothing = ???
}
//#component-logger-actor
