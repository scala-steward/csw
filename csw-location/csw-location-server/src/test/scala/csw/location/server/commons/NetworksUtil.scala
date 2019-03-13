package csw.location.server.commons
import java.net.{Inet6Address, InetAddress, NetworkInterface}

import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter

class NetworksUtil {
  def hostname: String =
    NetworkInterface.getNetworkInterfaces.asScala.toList
      .flatMap(iface => iface.getInetAddresses.asScala.toList)
      .find(ip => isIpv4(ip))
      .getOrElse(InetAddress.getLocalHost)
      .getHostAddress

  // Check if the given InetAddress is not a loopback address and is a ipv4 address
  private def isIpv4(addr: InetAddress): Boolean =
    // Don't use ipv6 addresses yet, since it seems to not be working with the current akka version
    !addr.isLoopbackAddress && !addr.isInstanceOf[Inet6Address]
}

object NetworksUtil {
  def apply(): NetworksUtil = new NetworksUtil()
}
