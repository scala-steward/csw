package csw.common.framework.internal.configparser

import com.typesafe.config.ConfigFactory
import csw.common.framework.models.{ComponentInfo, DoNotRegister}
import csw.services.location.models.ComponentType.Assembly
import csw.services.location.models.Connection
import org.scalatest.{FunSuite, Matchers}
import pureconfig.{CamelCase, ConfigConvert, ConfigFieldMapping, ProductHint}

class QuickParsingTest extends FunSuite with Matchers {

  val connections = Set(Connection.from("HCD2A-hcd-akka"), Connection.from("HCD2C-hcd-akka"))

  test("ee") {
    val conf             = ConfigFactory.parseString("""
        {
          name = "Assembly-1"
          componentType = assembly
          className = csw.pkgDemo.assembly1.Assembly1
          prefix = tcs.mobie.blue.filter
          locationServiceUsage = DoNotRegister
          connections = [
           {
             name: HCD2A
             componentType: hcd
             connectionType: akka
           },
           {
             name: HCD2C
             componentType: hcd
             connectionType: akka
           }
         ]
        }""".stripMargin)
    implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))
    ConfigConvert[ComponentInfo].from(conf.root()) shouldEqual Right(
      ComponentInfo(
        "Assembly-1",
        Assembly,
        "tcs.mobie.blue.filter",
        "csw.pkgDemo.assembly1.Assembly1",
        DoNotRegister,
        Set(Connection.from("HCD2A-hcd-akka"), Connection.from("HCD2C-hcd-akka"))
      )
    )
  }
}
