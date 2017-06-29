package csw.services.logging.internal

import csw.services.logging.internal.LoggingLevels._
import csw.services.logging.macros.DefaultSourceLocation

import scala.collection.mutable

/**
  * Created by gillies on 6/29/17.
  */
private[logging] trait ComponentLoggingState {

  // Queue of messages sent before logger is started
  private[logging] val msgs = new mutable.Queue[LogActorMessages]()

  // Deal with messages send before logger was ready
  /*
  msgs.synchronized {
    if (msgs.nonEmpty) {
      log.info(s"Saw ${msgs.size} messages before logger start")(() => DefaultSourceLocation)
      for (msg <- msgs) {
        MessageHandler.sendMsg(msg)
      }
    }
    msgs.clear()
  }
  */

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
