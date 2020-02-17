package csw.logging.client.compat

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import csw.logging.models.Level.INFO
import csw.logging.client.commons.LoggingKeys
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.utils.LoggingTestSuite
import csw.logging.models.Level
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

class MyActor extends Actor with ActorLogging {
  val exception = new RuntimeException("Exception occurred")

  def receive = {
    case "info"  => log.info("info")
    case "debug" => log.debug("debug")
    case "warn"  => log.warning("warn")
    case "error" => log.error(exception, "error")
  }
}
// CSW-78: PrefixRedesign for logging
class AkkaLoggerTest extends LoggingTestSuite with AnyFunSuiteLike with Matchers {

  test("logging framework should capture akka log messages and log it") {
    val actorRef  = ActorSystem("test1").actorOf(Props(new MyActor()), "my-actor")
    val className = classOf[MyActor].getName

    actorRef ! "info"
    actorRef ! "debug"
    actorRef ! "warn"
    actorRef ! "error"

    Thread.sleep(300)

    logBuffer.foreach { log =>
      log.contains(LoggingKeys.COMPONENT_NAME) shouldBe false
      log.contains(LoggingKeys.PREFIX) shouldBe false
      log.contains(LoggingKeys.SUBSYSTEM) shouldBe false

      log.contains(LoggingKeys.SEVERITY) shouldBe true
      val currentLogLevel = log.getString(LoggingKeys.SEVERITY).toLowerCase
      Level(currentLogLevel) >= INFO shouldBe true

      log.getString(LoggingKeys.MESSAGE) shouldBe currentLogLevel
      log.getString(LoggingKeys.ACTOR) shouldBe actorRef.path.toString
      log.getString(LoggingKeys.CLASS) shouldBe className
      log.getString(LoggingKeys.KIND) shouldBe "akka"
    }

  }

}
