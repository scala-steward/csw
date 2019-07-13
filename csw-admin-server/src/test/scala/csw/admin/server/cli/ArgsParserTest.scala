package csw.admin.server.cli

import java.io.ByteArrayOutputStream

import org.scalatest.{FunSuite, Matchers}

class ArgsParserTest extends FunSuite with Matchers {

  //Capture output/error generated by the parser, for cleaner test output. If interested, errCapture.toString will return capture errors.
  val outCapture = new ByteArrayOutputStream
  val errCapture = new ByteArrayOutputStream
  val parser     = new ArgsParser("csw-admin-server")

  def silentParse(args: Array[String]): Option[Options] =
    Console.withOut(outCapture) {
      Console.withErr(errCapture) {
        parser.parse(args.toList)
      }
    }

  test("parse without arguments") {
    val args = Array("")
    silentParse(args) shouldBe None
  }

  test("parse when only port argument provided") {
    val args = Array("--port", "8080")
    silentParse(args) shouldBe Some(Options(Some(8080)))
  }

  test("parse with all arguments") {
    val args = Array("--port", "8080", "--locationHost", "location.server")
    silentParse(args) shouldBe Some(Options(Some(8080), "location.server"))
  }
}
