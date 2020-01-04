package csw.framework.javadsl

import csw.framework.deploy.hostconfig.HostConfig
import csw.prefix.models.Subsystem

// $COVERAGE-OFF$
/**
 * Helper instance for Java to start `HostConfig` app
 */
object JHostConfig {

  /**
   * Utility for starting multiple Containers on a single host machine
   *
   * @param name the name to be used for the main app which uses this utility
   * @param args the command line args accepted in the main app which uses this utility
   */
  def start(name: String, subsystem: Subsystem, args: Array[String]): Unit =
    HostConfig.start(name: String, subsystem, args: Array[String])

}
// $COVERAGE-ON$
