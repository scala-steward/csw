package csw.location.client
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.scaladsl.LocationService
import csw.location.client.internal.{LocationServiceClient, Settings}
import csw.location.client.scaladsl.HttpLocationServiceFactory

import scala.concurrent.{ExecutionContext, Future}

object LocationParallel {
  lazy val config: Config                              = ConfigFactory.load()
  lazy val settings                                    = new Settings(config)
  lazy implicit val system: ActorSystem                = ActorSystemFactory.remote()
  lazy implicit val materializer: Materializer         = ActorMaterializer()
  lazy implicit val executionContext: ExecutionContext = system.dispatcher

  lazy val locationService: LocationServiceClient = HttpLocationServiceFactory.makeLocalClient.asInstanceOf[LocationServiceClient]

  def main(args: Array[String]): Unit = {
    Future
      .traverse((1 to 5).toList) { x ⇒
        val connection = HttpConnection(ComponentId(s"$x@TestServer", ComponentType.Service))
        locationService.track1(connection).runForeach(xx ⇒ println(s"++++++++++++ $xx"))
      }
      .onComplete(println)
    Thread.sleep(1000)
  }

}
