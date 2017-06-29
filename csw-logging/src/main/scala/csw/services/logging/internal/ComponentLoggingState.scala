package csw.services.logging.internal

import csw.services.logging.internal.LoggingLevels._

/**
  * Created by gillies on 6/29/17.
  */
private[logging] trait ComponentLoggingState {



  //private[this] val loggingConfig = system.settings.config.getConfig("csw-logging")

  //private[this] val levels = loggingConfig.getString("logLevel")

  /*
  private[this] val defaultLevel: Level = if (Level.hasLevel(levels)) {
    Level(levels)
  } else {
    throw new Exception("Bad value for csw-logging.logLevel")
  }
  */
  val defaultLevel = Level("DEBUG")  // Temporary while trying to get working

  var logLevel: Level = defaultLevel




  @volatile var doTrace: Boolean = false
  @volatile var doDebug: Boolean = false
  @volatile var doInfo: Boolean = true
  @volatile var doWarn: Boolean = true
  @volatile var doError: Boolean = true

  /**
    * Get logging levels.
    *
    * @return the current and default logging levels.
    */
  //def getLevel: Levels = Levels(logLevel, defaultLevel)

  /**
    * Changes the logger API logging level.
    *
    * @param level the new logging level for the logger API.
    */
  def setLevel(level: Level): Unit = {
    logLevel = level
    doTrace = level.pos <= TRACE.pos
    doDebug = level.pos <= DEBUG.pos
    doInfo = level.pos <= INFO.pos
    doWarn = level.pos <= WARN.pos
    doError = level.pos <= ERROR.pos
  }
}
