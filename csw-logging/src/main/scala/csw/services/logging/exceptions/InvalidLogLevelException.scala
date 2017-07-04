package csw.services.logging.exceptions

import csw.services.logging.internal.LoggingLevels

case class InvalidLogLevelException(invalidLogLevel: String, allowedLogLevels: Seq[LoggingLevels.Level])
    extends RuntimeException(
        s"LogLevel $invalidLogLevel not supported. Allowed logging levels are [${allowedLogLevels.mkString(",")}]")
