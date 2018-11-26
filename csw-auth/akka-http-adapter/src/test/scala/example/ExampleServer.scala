package example
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.http.scaladsl.unmarshalling.GenericUnmarshallers
import csw.auth.adapters.akka.http.{Authentication, SecurityDirectives}
import csw.auth.core.token.TokenFactory
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

object ExampleServer extends HttpApp with App with GenericUnmarshallers with PlayJsonSupport {

  private val directives = SecurityDirectives(new Authentication(new TokenFactory))

  import directives._
  private val HOST = "localhost"
  private val PORT = 9004

  override protected def routes: Route = path("test") {
    secure { _ ⇒
      get { complete("secure get ok") }
    } ~
    post {
      complete("unsecure post ok")
    }
  }

  startServer(HOST, PORT)
}
