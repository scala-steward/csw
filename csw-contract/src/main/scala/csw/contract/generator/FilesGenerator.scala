package csw.contract.generator

import java.nio.file.{Files, Paths}

import io.bullet.borer.Encoder

object FilesGenerator extends ContractCodecs {

  def generate(services: Services, outputPath: String): Unit = {
    services.data.foreach {
      case (serviceName, service) =>
        writeData(s"$outputPath/$serviceName/endpoints", "http-endpoints", service.`http-endpoints`)
        writeData(s"$outputPath/$serviceName/endpoints", "websocket-endpoints", service.`websocket-endpoints`)
        service.models.foreach {
          case (modelName, model) => writeData(s"$outputPath/$serviceName/models", modelName, model)
        }
    }
  }

  def generateEntireJson(services: Services, outputPath: String): Unit = {
    writeData(s"$outputPath", "serviceData", services.data)
  }

  def writeData[T: Encoder](dir: String, fileName: String, data: T): Unit = {
    Files.createDirectories(Paths.get(dir))
    Files.writeString(Paths.get(dir, s"$fileName.json"), JsonHelper.toJson(data))
  }
}
