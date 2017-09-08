package csw.common.framework.internal.configparser

import com.typesafe.config.{Config, ConfigRenderOptions}
import csw.common.framework.models.{ComponentInfo, ContainerInfo}
import io.circe.Decoder
import io.circe.parser.decode

object ComponentInfoParser {

  def parseComponent(config: Config): ComponentInfo  = parse[ComponentInfo](config)
  def parseContainer(config: Config): ContainerInfo  = parse[ContainerInfo](config)
  def parseStandalone(config: Config): ComponentInfo = parseComponent(config)

  private def parse[T: Decoder](config: Config): T = {
    decode[T](configToJsValue(config)) match {
      case Left(error) ⇒ throw new RuntimeException(error.fillInStackTrace())
      case Right(info) ⇒ info
    }
  }

  private def configToJsValue(config: Config): String = {
    config.root().render(ConfigRenderOptions.concise())
  }
}
