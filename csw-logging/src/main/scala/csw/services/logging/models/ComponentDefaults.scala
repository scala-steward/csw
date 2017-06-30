package csw.services.logging.models

import com.typesafe.config.Config
import csw.services.logging.internal.LoggingLevels.Level

import scala.collection.JavaConverters._
import scala.util.Try


/**
  * Represents component initial logging levels as a Map of componentName as key and Level as value.
  * Initial values are used to control log levels for different components at startup
  *
  * @param defaults
  */
case class ComponentDefaults(defaults: Map[String, Level]) {

  def add(name: String, level: Level): ComponentDefaults = ComponentDefaults(defaults + (name → level))
}

object ComponentDefaults {

  /**
    * Extracts the set of component default logging levels configured in logging configuration
    *
    * @param loggingConfig the logging configuration object
    * @return Set of component logging defaults
    */
  def from(loggingConfig: Config): ComponentDefaults = ComponentDefaults {
    Try {
      loggingConfig
        .getObject("component-default-logging-levels")
        .unwrapped()
        .asScala
        .map {
          case (name, logLevel) ⇒ {
            (name, Level(logLevel.toString))
          }
        }
        .toMap
    }.getOrElse(Map.empty)
  }
}