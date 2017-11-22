package csw.services.logging.commons

import csw.messages.framework.ComponentInfo
import csw.messages.framework.LocationServiceUsage.DoNotRegister
import csw.messages.location.ComponentType.Service

object ComponentInfoFactory {
  def make(name: String): ComponentInfo = new ComponentInfo(name, Service, "", "", DoNotRegister)
}
