package csw.services.logging.scaladsl

import com.typesafe.config.ConfigFactory
import csw.services.logging.internal.LoggingLevels.Level
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class LoggingSystemTest extends FunSuite with Matchers with BeforeAndAfterAll {

  // This will load default filter set in application.conf file if provided
  private val loggingSystem = LoggingSystemFactory.start()
  private val config        = ConfigFactory.load().getConfig("csw-logging")

  override protected def afterAll(): Unit =
    Await.result(loggingSystem.stop, 10.seconds)

  test("should load default log level for akka and slf4j provided in configuration file") {
    loggingSystem.getAkkaLevel.default.name shouldBe config.getString("akkaLogLevel").toUpperCase
    loggingSystem.getSlf4jLevel.default.name shouldBe config.getString("slf4jLogLevel").toUpperCase
  }

  test("should able to set log level for slf4j and akka") {
    val akkaLogLevel  = "Error"
    val slf4jLogLevel = "INFO"

    loggingSystem.setAkkaLevel(Level(akkaLogLevel))
    loggingSystem.setSlf4jLevel(Level(slf4jLogLevel))

    loggingSystem.getAkkaLevel.current.name.toLowerCase shouldBe akkaLogLevel.toLowerCase
    loggingSystem.getSlf4jLevel.current.name.toLowerCase shouldBe slf4jLogLevel.toLowerCase
  }
}
