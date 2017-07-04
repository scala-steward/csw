package csw.services.logging.internal

import com.typesafe.config.ConfigFactory
import csw.services.logging.internal.LoggingLevels._
import csw.services.logging.models.LogMetadata

import scala.util.Try

private[logging] class ComponentLoggingState(componentName: String) {

  private[this] val loggingConfig = ConfigFactory.load().getConfig("csw-logging")
  // Get the default log level from the configuration file.
  // This is used when component does not specify their initial log level inside config block : component-default-logging-levels
  private[this] lazy val defaultLogLevel = loggingConfig.getString("logLevel")

  private[this] def akkaLogLevel = LoggingState.akkaLogLevel.getOrElse(Level(loggingConfig.getString("akkaLogLevel")))

  private[this] def slf4jLogLevel =
    LoggingState.slf4jLogLevel.getOrElse(Level(loggingConfig.getString("slf4jLogLevel")))

  private[this] val logLevel = Try {
    loggingConfig.getConfig("component-default-logging-levels").getString(componentName)
  }.getOrElse(defaultLogLevel)

  private[this] var componentLogLevel = Level(logLevel)

  @volatile var doTrace: Boolean = false
  @volatile var doDebug: Boolean = false
  @volatile var doInfo: Boolean  = true
  @volatile var doWarn: Boolean  = true
  @volatile var doError: Boolean = true

  setComponentLevel(componentLogLevel)

  /**
   * Set component log level
   *
   * @param level the new logging level for the logger API.
   */
  def setComponentLevel(level: Level): Unit = {
    componentLogLevel = level
    doTrace = level.pos <= TRACE.pos
    doDebug = level.pos <= DEBUG.pos
    doInfo = level.pos <= INFO.pos
    doWarn = level.pos <= WARN.pos
    doError = level.pos <= ERROR.pos
  }

  /**
   * Get the current log configuration of component.
   *
   * @return LogMetadata of component
   */
  def getComponentMetadata: LogMetadata = LogMetadata(componentLogLevel, akkaLogLevel, slf4jLogLevel)

}
