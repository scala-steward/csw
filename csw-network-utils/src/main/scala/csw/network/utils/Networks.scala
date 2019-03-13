package csw.network.utils

import java.net.{Inet6Address, InetAddress, NetworkInterface}

import csw.logging.api.scaladsl.Logger
import csw.network.utils.commons.NetworksLogger
import csw.network.utils.exceptions.{NetworkInterfaceNotFound, NetworkInterfaceNotProvided}

import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter

/**
 * Picks an appropriate ipv4 address to register using the NetworkInterfaceProvider
 *
 * @param interfaceName provide the name of network interface where csw cluster is running
 */
case class Networks(interfaceName: String) {

  private val log: Logger = NetworksLogger.getLogger

  /**
   * Gives the ipv4 host address
   */
  def hostname: String = ipv4Address.getHostAddress

  /**
   * Gives the non-loopback, ipv4 address for the given network interface. If no interface name is provided then the address mapped
   * to the first available interface is chosen.
   */
  private[network] def ipv4Address: InetAddress =
    inetAddresses
      .find(ip => isIpv4(ip))
      .getOrElse(InetAddress.getLocalHost)

  // Check if the given InetAddress is not a loopback address and is a ipv4 address
  private def isIpv4(addr: InetAddress): Boolean =
    // Don't use ipv6 addresses yet, since it seems to not be working with the current akka version
    !addr.isLoopbackAddress && !addr.isInstanceOf[Inet6Address]

  private def inetAddresses: List[InetAddress] =
    if (interfaceName.isEmpty) {
      val networkInterfaceNotProvided =
        NetworkInterfaceNotProvided("interfaceName not provided in environment variables or system properties")

      log.error(networkInterfaceNotProvided.getMessage, ex = networkInterfaceNotProvided)
      throw networkInterfaceNotProvided
    } else getInterface(interfaceName)

  /**
   * Get List of InetAddress for a given interface
   */
  private def getInterface(interfaceName: String): List[InetAddress] =
    Option(NetworkInterface.getByName(interfaceName)) match {
      case Some(nic) => nic.getInetAddresses.asScala.toList
      case None =>
        val networkInterfaceNotFound = NetworkInterfaceNotFound(s"Network interface=$interfaceName not found")
        log.error(networkInterfaceNotFound.getMessage, ex = networkInterfaceNotFound)
        throw networkInterfaceNotFound
    }

}

object Networks {

  /**
   * Picks an appropriate ipv4 address from the network interface provided.
   * If no specific network interface is provided, a [[NetworkInterfaceNotProvided]] exception is thrown
   */
  def apply(interfaceName: String): Networks = new Networks(interfaceName)

  def apply(): Networks = new Networks((sys.env ++ sys.props).getOrElse("interfaceName", ""))
}
