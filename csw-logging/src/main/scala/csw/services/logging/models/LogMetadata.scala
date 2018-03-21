package csw.services.logging.models

import csw.common.TMTSerializable
import csw.services.logging.internal.LoggingLevels.Level

/**
 * Holds metadata information about logging configuration
 */
case class LogMetadata(defaultLevel: Level, akkaLevel: Level, slf4jLevel: Level, componentLevel: Level) extends TMTSerializable
