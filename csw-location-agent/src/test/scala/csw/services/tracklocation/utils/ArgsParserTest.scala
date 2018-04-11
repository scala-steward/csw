package csw.services.tracklocation.utils

import java.io.ByteArrayOutputStream

import csw.services.tracklocation.models.Options
import org.scalatest.{BeforeAndAfterEach, FunSuite, Matchers}

class ArgsParserTest extends FunSuite with Matchers with BeforeAndAfterEach {

  //Capture output/error generated by the parser, for cleaner test output. If interested, errCapture.toString will return capture errors.
  val outCapture = new ByteArrayOutputStream
  val errCapture = new ByteArrayOutputStream

  override protected def afterEach(): Unit = {
    outCapture.reset()
    errCapture.reset()
  }

  def silentParse(args: Array[String]): Option[Options] =
    Console.withOut(outCapture) {
      Console.withErr(errCapture) {
        new ArgsParser("csw-location-agent").parser.parse(args, Options())
      }
    }

  test("test parser with valid arguments") {
    val port     = 5555
    val services = "redis,alarm,watchdog"
    val args     = Array("--name", services, "--port", port.toString, "--command", "sleep 5")

    val x: Option[Options] = silentParse(args)
    x should contain(Options(List("redis", "alarm", "watchdog"), Some("sleep 5"), Some(port), None, None, false))
  }

  test("test parser with invalid service name combinations") {
    val port = 5555
    val listOfInvalidServices: List[String] =
      List("re-dis,alarm", "redis, alarm", " redis,alarm-service", "redis, alarm ", "redis, ,alarm")

    listOfInvalidServices.foreach { service =>
      val args = Array("--name", service, "--port", port.toString, "--command", "sleep 5")
      silentParse(args) shouldEqual None
    }
  }

  test("test parser with only --name option, should be allowed") {
    val args               = Array[String]("--name", "myService")
    val x: Option[Options] = silentParse(args)

    x should contain(new Options(List("myService"), None, None, None, None, false))
  }

  test("test parser without --name option, should error out") {
    val args               = Array[String]("abcd")
    val x: Option[Options] = silentParse(args)

    x shouldEqual None
  }

  test("test parser with service name containing '-' character, should error out") {
    val args               = Array[String]("--name", "alarm-service")
    val x: Option[Options] = silentParse(args)
    x shouldEqual None
  }

  test("test parser with list of services containing leading/trailing whitespace, should error out") {
    val args               = Array[String]("--name", "   redis-server,   alarm,watchdog   ")
    val x: Option[Options] = silentParse(args)

    x shouldEqual None
  }

  test("test parser with service name containing leading whitespace, should error out") {
    val args               = Array[String]("--name", " someService")
    val x: Option[Options] = silentParse(args)

    x shouldEqual None
  }

  test("test parser with service name containing trailing whitespace, should error out") {
    val args               = Array[String]("--name", "someService ")
    val x: Option[Options] = silentParse(args)

    x shouldEqual None
  }

  test("test parser with service name containing both leading and trailing whitespaces, error is shown") {
    val args               = Array[String]("--name", " someService ")
    val x: Option[Options] = silentParse(args)

    x shouldEqual None
  }
}
