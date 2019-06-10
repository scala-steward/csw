/*
package csw.config.server.http.routes

import java.nio.file.{Path, Paths}
import java.time.Instant

import akka.http.scaladsl.server.Route
import csw.config.api.models.{ConfigData, ConfigId}
import csw.config.api.scaladsl.ConfigService
import csw.config.server.ActorRuntime
import csw.config.server.svn.SvnConfigServiceFactory

import scala.concurrent.Future

class ConfigEndpointRoutes(
    configServiceFactory: SvnConfigServiceFactory,
    actorRuntime: ActorRuntime
) {

  import actorRuntime._
  import io.circe.generic.auto._
  import tapir.Codec.PlainCodec
  import tapir._
  import tapir.docs.openapi._
  import tapir.json.circe._
  import tapir.openapi.circe.yaml._
  import tapir.server.akkahttp._

  private val UnknownUser = "Unknown"
  private val AdminRole   = "admin"

  private def configService(userName: String = UnknownUser): ConfigService = configServiceFactory.make(userName)

  case class FilePath(path: String)

  private val baseEndpoint: Endpoint[FilePath, String, Future[Option[ConfigData]], Nothing] =
    endpoint
      .in("config")
      .in(path[String]("filepath").description("Store file at provided file path in repository").mapTo(FilePath))
      .out(jsonBody[Future[Option[ConfigData]]])
      .errorOutput(stringBody)

  private val baseEndpoint1: Endpoint[FilePath, String, ConfigData, Nothing] =
    endpoint
      .in("config")
      .in(path[String]("filepath").description("Store file at provided file path in repository").mapTo(FilePath))
      .out(jsonBody[ConfigData])
      .errorOutput(stringBody)

  case class GetQuery(date: Option[Instant], id: Option[ConfigId])
  implicit val dateCodec: PlainCodec[Instant] = Codec.stringPlainCodecUtf8.map(Instant.parse)(_.toString)
  implicit val idCodec: PlainCodec[ConfigId]  = Codec.stringPlainCodecUtf8.map(ConfigId(_))(_.toString)
  private val getQueryInput: EndpointInput[GetQuery] =
    query[Option[Instant]]("date")
      .description("Latest version of file on provided timestamp will be retrieved.")
      .and(query[Option[ConfigId]]("id").description("Revision number of configuration file."))
      .mapTo(GetQuery)
  private val getEndpoint: Endpoint[(FilePath, GetQuery), String, Future[Option[ConfigData]], Nothing] = baseEndpoint.get
    .in(getQueryInput)
    .description("Fetches the latest version of requested configuration file from the repository either from normal/annex store.")

  private val getEndpoint1: Endpoint[(FilePath, GetQuery), String, ConfigData, Nothing] = baseEndpoint1.get
    .in(getQueryInput)
    .description("Fetches the latest version of requested configuration file from the repository either from normal/annex store.")

  private def getConfig(filePath: FilePath, getQuery: GetQuery): Future[Either[String, Future[Option[ConfigData]]]] = Future {
    val a: Either[String, Future[Option[ConfigData]]] = if (filePath.path.nonEmpty) {
      val path = Paths.get(filePath.path)
      (getQuery.date, getQuery.id) match {
        case (Some(date), _) ⇒
          Right[String, Future[Option[ConfigData]]](configService().getByTime(path, date.asInstanceOf[Instant]))
        case (_, Some(id)) ⇒ Right[String, Future[Option[ConfigData]]](configService().getById(path, id.asInstanceOf[ConfigId]))
        case (_, _)        ⇒ Right[String, Future[Option[ConfigData]]](configService().getLatest(path))
      }
    } else Left[String, Future[Option[ConfigData]]]("Filepath must be provided")
    a
  }

  private def getConfig1(filePath: FilePath, getQuery: GetQuery): Future[Either[String, ConfigData]] = Future {
    val a: Either[String, ConfigData] = if (filePath.path.nonEmpty) {
      val path = Paths.get(filePath.path)
      (getQuery.date, getQuery.id) match {
        case (_, _) ⇒ Right[String, ConfigData](ConfigData.fromString(""))
      }
    } else Left[String, ConfigData]("Filepath must be provided")
    a
  }

  def getConfigRoute: Route  = getEndpoint.toRoute((getConfig _).tupled)
  def getConfigRoute1: Route = getEndpoint1.toRoute((getConfig1 _).tupled)

  def yaml: String = List(getEndpoint).toOpenAPI("api", "config-service-0.1").toYaml

}
 */
