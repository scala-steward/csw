package csw.config.server.http

import akka.Done
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import csw.aas.http.AuthorizationPolicy.{ClientRolePolicy, EmptyPolicy}
import csw.aas.http.SecurityDirectives
import csw.config.api.scaladsl.ConfigService
import csw.config.server.ActorRuntime
import csw.config.server.svn.SvnConfigServiceFactory
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

import scala.concurrent.Future

/**
 * Routes supported by config server
 *
 * @param configServiceFactory factory to create config service to which the routes will delegate operations
 * @param actorRuntime actorRuntime provides runtime accessories related to ActorSystem like Materializer, ExecutionContext etc.
 * @param configHandlers exception handler which maps server side exceptions to Http Status codes
 */
class ConfigServiceRoute(
    configServiceFactory: SvnConfigServiceFactory,
    actorRuntime: ActorRuntime,
    configHandlers: ConfigHandlers,
    securityDirectives: SecurityDirectives
) extends HttpSupport {

  import actorRuntime._
  import io.circe.generic.auto._
  import securityDirectives._
  import tapir._
  import tapir.json.circe._
  import tapir.server.akkahttp._

//  val configEndpointRoute = new ConfigEndpointRoutes(configServiceFactory, actorRuntime)

  private val UnknownUser = "Unknown"
  private val AdminRole   = "admin"

  private def configService(userName: String = UnknownUser): ConfigService = configServiceFactory.make(userName)

  def baseEndpoint: Endpoint[Unit, Unit, String, Nothing] = endpoint.out(stringBody).in("test")

  case class Custom(msg: String)
  def testHelloEndpoint: Endpoint[Unit, Unit, String, Nothing] =
    baseEndpoint.post
      .in("hello")

  def route: Route = cors() {
    routeLogger {
      handleExceptions(configHandlers.jsonExceptionHandler) {
        handleRejections(configHandlers.jsonRejectionHandler) {
          sMethod2(EmptyPolicy) {
            post {
              testHelloEndpoint.toRoute { _ ⇒
                Future.successful(Right("Okay secure"))
              }
            }
          } ~
          prefix("config") { filePath ⇒
            /*(get & rejectEmptyResponse) { // fetch the file - http://{{hostname}}:{{port}}/config/{{path}}
              (dateParam & idParam) {
                case (Some(date), _) ⇒ complete(configService().getByTime(filePath, date))
                case (_, Some(id))   ⇒ complete(configService().getById(filePath, id))
                case (_, _)          ⇒ complete(configService().getLatest(filePath))
              }
            } ~*/
            head { // check if file exists - http://{{hostname}}:{{port}}/config/{{path}}
              idParam { id ⇒
                complete {
                  configService().exists(filePath, id).map { found ⇒
                    if (found) StatusCodes.OK else StatusCodes.NotFound
                  }
                }
              }
            } ~
            sPost(ClientRolePolicy(AdminRole)) { token =>
              (configDataEntity & annexParam & commentParam) { (configData, annex, comment) ⇒
                complete(
                  StatusCodes.Created -> configService(token.userOrClientName).create(filePath, configData, annex, comment)
                )
              }
            } ~
            sPut(ClientRolePolicy(AdminRole)) { token =>
              (configDataEntity & commentParam) { (configData, comment) ⇒
                complete(configService(token.userOrClientName).update(filePath, configData, comment))
              }
            } ~
            sDelete(ClientRolePolicy(AdminRole)) { token =>
              commentParam { comment ⇒
                complete(configService(token.userOrClientName).delete(filePath, comment).map(_ ⇒ Done))
              }
            }
          } ~
          (prefix("active-config") & get & rejectEmptyResponse) { filePath ⇒
            dateParam { // fetch the currently active file - http://{{hostname}}:{{port}}/active-config/{{path}}
              case Some(date) ⇒
                complete(configService().getActiveByTime(filePath, date))
              case _ ⇒ complete(configService().getActive(filePath))
            }
          } ~
          prefix("active-version") { filePath ⇒
            (get & rejectEmptyResponse) { // fetch the active version - http://{{hostname}}:{{port}}/active-version/{{path}}
              complete(configService().getActiveVersion(filePath))
            } ~
            sPut(ClientRolePolicy(AdminRole)) { token =>
              (idParam & commentParam) {
                case (Some(configId), comment) ⇒
                  complete(configService(token.userOrClientName).setActiveVersion(filePath, configId, comment).map(_ ⇒ Done))
                case (_, comment) ⇒
                  complete(configService(token.userOrClientName).resetActiveVersion(filePath, comment).map(_ ⇒ Done))
              }
            }
          } ~
          (prefix("history") & get) { filePath ⇒
            (maxResultsParam & fromParam & toParam) { (maxCount, from, to) ⇒ // fetch the history of file - http://{{hostname}}:{{port}}/history/{{path}}
              complete(configService().history(filePath, from, to, maxCount))
            }
          } ~
          (prefix("history-active") & get) { filePath ⇒ // fetch the history of active version - http://{{hostname}}:{{port}}/history-active/{{path}}
            (maxResultsParam & fromParam & toParam) { (maxCount, from, to) ⇒
              complete(configService().historyActive(filePath, from, to, maxCount))
            }
          } ~
          (path("list") & get) { // list all files based on file type i.e.'Normal' or 'Annex' and/or pattern if provided - http://{{hostname}}:{{port}}/list
            (typeParam & patternParam) { (fileType, pattern) ⇒
              complete(configService().list(fileType, pattern))
            }
          } ~
          (path("metadata") & get) { // fetch the metadata of config server - http://{{hostname}}:{{port}}/metadata
            complete(configService().getMetadata)
          }
        }
      }
    }
  }
}
