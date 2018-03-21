package csw.services.config.client.commons

import java.net.URI

import csw.common.location.Connection.HttpConnection
import csw.common.location.HttpLocation

object LocationFactory {
  def http(connection: HttpConnection, uri: URI) = HttpLocation(connection, uri, null)
}
