package csw.common.framework.internal.configparser

import com.typesafe.config.Config
import csw.common.framework.models.{ComponentInfo, ContainerInfo}
import pureconfig.error.ConfigReaderException
import pureconfig.{CamelCase, ConfigConvert, ConfigFieldMapping, ProductHint}

object ComponentInfoParser {

  implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))
  def parseContainer(config: Config): ContainerInfo = {
    ConfigConvert[ContainerInfo].from(config.root()) match {
      case Left(errors) ⇒ throw new ConfigReaderException[ContainerInfo](errors)
      case Right(info)  ⇒ info
    }
  }

  def parseComponent(config: Config): ComponentInfo = {
    ConfigConvert[ComponentInfo].from(config.root()) match {
      case Left(errors) ⇒ throw new ConfigReaderException[ComponentInfo](errors)
      case Right(info)  ⇒ info
    }
  }
  def parseStandalone(config: Config): ComponentInfo = parseComponent(config)
}
