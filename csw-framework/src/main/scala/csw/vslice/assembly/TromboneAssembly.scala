package csw.vslice.assembly

import java.io.File

/**
 * All assembly messages are indicated here
 */
object TromboneAssembly {

  // Get the trombone config file from the config service, or use the given resource file if that doesn't work
  val tromboneConfigFile = new File("trombone/tromboneAssembly.conf")
  val resource           = new File("tromboneAssembly.conf")

//  def props(assemblyInfo: AssemblyInfo, supervisor: ActorRef) = Props(new TromboneAssembly(assemblyInfo, supervisor))

  sealed trait TromboneAssemblyMsg
  // Used internally to start/stop  commands
  private[assembly] case object CommandStart       extends TromboneAssemblyMsg
  private[assembly] case object StopCurrentCommand extends TromboneAssemblyMsg

//  private val badHCDReference = None
}
