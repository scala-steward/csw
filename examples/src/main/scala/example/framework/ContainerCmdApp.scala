package example.framework

import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem.CSW

//#container-app
object ContainerCmdApp extends App {

  ContainerCmd.start("ContainerCmdApp", CSW, args)

}
//#container-app
