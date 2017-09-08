package csw.common.framework.internal.configparser

import csw.common.framework.internal.configparser.ComponentInfoParser.configToJsValue
import csw.common.framework.models.LocationServiceUsage.{DoNotRegister, RegisterOnly}
import csw.common.framework.models.{ComponentInfo, ContainerInfo}
import csw.services.location.models.ComponentType.{Assembly, HCD}
import csw.services.location.models.Connection
import io.circe.parser._
import io.circe.syntax.EncoderOps
import org.scalatest.{FunSuite, Matchers}

class CirceBaseTest extends FunSuite with Matchers {

  private val hcd2A: ComponentInfo =
    ComponentInfo("HCD-2A", HCD, "tcs.mobie.blue.filter", "csw.pkgDemo.hcd2.Hcd2", RegisterOnly, Set.empty)
  private val hcd2B: ComponentInfo =
    ComponentInfo(
      "HCD-2B",
      HCD,
      "tcs.mobie.blue.disperser",
      "csw.pkgDemo.hcd2.Hcd2",
      DoNotRegister,
      Set.empty
    )

  private val container = ContainerInfo("Container-1", RegisterOnly, Set(hcd2A, hcd2B))

  private val assembly: ComponentInfo = ComponentInfo(
    "Assembly-1",
    Assembly,
    "tcs.mobie.blue.filter",
    "csw.pkgDemo.assembly1.Assembly1",
    DoNotRegister,
    Set(Connection.from("HCD2A-hcd-akka"), Connection.from("HCD2C-hcd-akka"))
  )

  test("test ComponentInfo") {

    val jsonComponent = assembly.asJson.toString
    val info: ComponentInfo = decode[ComponentInfo](jsonComponent) match {
      case Left(error) ⇒ throw new RuntimeException(error.fillInStackTrace())
      case Right(info) ⇒ info
    }

    info shouldBe assembly
  }

  test("test ContainerInfo") {

    println(container.asJson.toString)

    val jsonContainer = container.asJson.toString
    val info: ContainerInfo = decode[ContainerInfo](jsonContainer) match {
      case Left(error) ⇒ throw new RuntimeException(error.fillInStackTrace())
      case Right(info) ⇒ info
    }

    info shouldBe container
  }

  test("test default values") {

    case class Person(name: String, age: Int = 100)

    import io.circe.generic.semiauto._
    import io.circe.{Decoder, ObjectEncoder}

    implicit val barDecoder: Decoder[Person]       = deriveDecoder[Person]
    implicit val barEncoder: ObjectEncoder[Person] = deriveEncoder[Person]

    val str = """{
                "name" : "abcd"
                }"""

    val person = decode[Person](str) match {
      case Left(error) ⇒ throw new RuntimeException(error.fillInStackTrace())
      case Right(info) ⇒ info
    }

    println(person)

  }

}
