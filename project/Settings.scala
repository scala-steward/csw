import com.typesafe.sbt.MultiJvmPlugin.MultiJvmKeys.MultiJvm
import sbt.Keys._
import sbt._

object Settings {

  def addAliases: Seq[Setting[_]] = {
    addCommandAlias(
      "testAll",
      "test; multi-jvm:test"
    ) ++
    addCommandAlias(
      "compileAll",
      ";set scalafmtCheck; scalastyle; test:compile; multi-jvm:compile"
    ) ++
    addCommandAlias(
      "buildAll",
      "; scalafmtCheck; scalastyle; clean; makeSite; test:compile; multi-jvm:compile"
    )
  }

  def multiJvmTestTask(multiJvmProjects: Seq[ProjectReference]): Seq[Setting[_]] = {
    val tasks: Seq[Def.Initialize[Task[Unit]]] = multiJvmProjects.map(p => p / MultiJvm / test)

    Seq(
      MultiJvm / test / aggregate := false,
      MultiJvm / test := Def.sequential(tasks).value
    )
  }

  // export ESW_TS_VERSION env variable which is compatible with csw
  // this represents version number of javascript docs maintained at https://github.com/tmtsoftware/esw-ts
  def eswTsVersion: String =
    (sys.env ++ sys.props).get("ESW_TS_VERSION") match {
      case Some(v) => v
      case None    => "0.1.0-SNAPSHOT"
    }
}
