package csw.common.framework.internal.configparser

import com.typesafe.config.Config
import csw.common.framework.models.{ComponentInfo, ContainerInfo}
import pureconfig.error.ConfigReaderException
import pureconfig.{CamelCase, ConfigConvert, ConfigFieldMapping, ProductHint}

object ComponentInfoParser {

  implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def parseContainer(config: Config): ContainerInfo  = parse[ContainerInfo](config)
  def parseComponent(config: Config): ComponentInfo  = parse[ComponentInfo](config)
  def parseStandalone(config: Config): ComponentInfo = parseComponent(config)

  private def parse[T](config: Config)(implicit converter: ConfigConvert[T]): T = {
    implicitly[ConfigConvert[T]].from(config.root()) match {
      case Left(errors) ⇒ throw new ConfigReaderException[ComponentInfo](errors)
      case Right(info)  ⇒ info
    }
  }
}
