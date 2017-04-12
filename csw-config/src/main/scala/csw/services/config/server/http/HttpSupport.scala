package csw.services.config.server.http

import java.nio.file.{Path, Paths}
import java.time.Instant

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.server.{Directive1, Directives}
import csw.services.config.api.models.{ConfigData, ConfigId}

trait HttpSupport extends Directives with JsonSupport {
  val pathParam: Directive1[Path] = parameter('path).map(filePath ⇒ Paths.get(filePath))
  val idParam: Directive1[Option[ConfigId]] = parameter('id.?).map(_.map(new ConfigId(_)))
  val dateParam: Directive1[Instant] = parameter('date).map(Instant.parse)
  val maxResultsParam: Directive1[Int] = parameter('maxResults.as[Int] ? Int.MaxValue)
  val commentParam: Directive1[String] = parameter('comment ? "")
  val oversizeParam: Directive1[Boolean] = parameter('oversize.as[Boolean] ? false)
  val fileDataParam: Directive1[ConfigData] = fileUpload("conf").map { case (_, source) ⇒ ConfigData.fromSource(source) }

  implicit val configDataMarshaller: ToEntityMarshaller[ConfigData] = Marshaller.opaque { configData =>
    Chunked.fromData(ContentTypes.`application/octet-stream`, configData.source)
  }
}
