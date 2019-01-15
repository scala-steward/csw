package csw.logging.core.appenders

import csw.logging.core.commons.LoggingKeys
import csw.logging.core.internal.LoggingLevels
import csw.logging.core.internal.LoggingLevels.Level
import csw.logging.core.utils.LoggingTestSuite
import org.slf4j.LoggerFactory

class Slf4jAppenderTest extends LoggingTestSuite {

  private val logger    = LoggerFactory.getLogger(classOf[Slf4jAppenderTest])
  private val className = getClass.getName

  test("logging framework should capture slf4j logs and log it") {

    logger.trace("trace")
    logger.debug("debug")
    logger.info("info")
    logger.warn("warn")

    Thread.sleep(300)

    logBuffer.foreach { log ⇒
      val currentLogLevel = log(LoggingKeys.SEVERITY).toString.toLowerCase
      Level(currentLogLevel) >= LoggingLevels.TRACE shouldBe true
      log(LoggingKeys.MESSAGE).toString shouldBe currentLogLevel
      log(LoggingKeys.CLASS).toString shouldBe className
      log(LoggingKeys.FILE).toString shouldBe "Slf4jAppenderTest.scala"
      log(LoggingKeys.KIND).toString shouldBe "slf4j"
    }
  }

}