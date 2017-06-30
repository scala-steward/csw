package csw.apps.clusterseed.admin.http

import java.io._

import akka.Done
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Source}
import csw.apps.clusterseed.admin.LogAdmin
import csw.apps.clusterseed.admin.internal.ActorRuntime

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.IMain

class AdminRoutes(adminExceptionHandler: AdminExceptionHandler, logAdmin: LogAdmin, actorRuntime: ActorRuntime)
    extends HttpSupport {

//  import actorRuntime._
  val settings = new Settings
  settings.usejavacp.value = true
  settings.deprecation.value = true
  settings.processArgumentString("-deprecation -feature -Xfatal-warnings -Xlint")

  //  the interpreter is used by the javax.script engine

  def read(bufferedReader: BufferedReader): String = {
    var str = ""
    while (bufferedReader.ready()) {
      str += bufferedReader.readLine()
    }
    str
  }

  def greeter: Flow[Message, Message, Any] = {
    val pipedWriter    = new PipedWriter()
    val pipedReader    = new PipedReader(pipedWriter)
    val bufferedReader = new BufferedReader(pipedReader)

    val printWriter = new PrintWriter(pipedWriter, true)
    val interpreter = new IMain(settings, printWriter)

    Flow[Message].mapConcat {
      case tm: TextMessage =>
        interpreter.interpret(tm.getStrictText)
        TextMessage(read(bufferedReader)) :: Nil
      case bm: BinaryMessage =>
        // ignore binary messages but drain content to avoid the stream being clogged
        bm.getStrictData
        Nil
    }
  }

  import actorRuntime._
  val route: Route = routeLogger {
    handleExceptions(adminExceptionHandler.exceptionHandler) {
      path("admin" / "logging" / Segment / "level") { componentName ⇒
        get {
          complete(logAdmin.getLogMetadata(componentName))
        } ~
        post {
          logLevelParam { (logLevel) ⇒
            complete(logAdmin.setLogLevel(componentName, logLevel).map(_ ⇒ Done))
          }
        }
      } ~
      path("greeter") {
        handleWebSocketMessages(greeter)
      }
    }
  }

}
