package csw.services.config.server.commons

import csw.common.location.Connection.HttpConnection
import csw.services.location.models.HttpRegistration

object RegistrationFactory {
  def http(connection: HttpConnection, port: Int, path: String) = HttpRegistration(connection, port, path, null)
}
