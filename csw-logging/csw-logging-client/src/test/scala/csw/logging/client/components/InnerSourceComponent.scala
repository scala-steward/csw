package csw.logging.client.components

import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix

object InnerSourceLogger extends LoggerFactory(Prefix("csw.InnerSourceComponent"))

class InnerSourceComponent {

  val log: Logger = InnerSourceLogger.getLogger

  def startLogging(logs: Map[String, String]): Unit = new InnerSource().startLogging(logs)

  class InnerSource {

    // Do not add any lines before this method
    // Tests are written to assert on this line numbers
    // In case any line needs to be added then update constants in companion object
    def startLogging(logs: Map[String, String]): Unit = {
      log.trace(logs("trace"))
      log.debug(logs("debug"))
      log.info(logs("info"))
      log.warn(logs("warn"))
      log.error(logs("error"))
      log.fatal(logs("fatal"))
    }
  }
}

object InnerSourceComponent {
  val TRACE_LINE_NO = 21
  val DEBUG_LINE_NO = TRACE_LINE_NO + 1
  val INFO_LINE_NO  = TRACE_LINE_NO + 2
  val WARN_LINE_NO  = TRACE_LINE_NO + 3
  val ERROR_LINE_NO = TRACE_LINE_NO + 4
  val FATAL_LINE_NO = TRACE_LINE_NO + 5
}
