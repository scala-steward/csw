package csw.location.client
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.client.internal.{LocationServiceClient, Settings}
import csw.location.client.scaladsl.HttpLocationServiceFactory

import scala.concurrent.{ExecutionContext, Future}

/*
Link of document where observations of spike are summarized
https://docs.google.com/document/d/1o5ObXcD5lzNzW68jwyjFDnpy5CsY_iMtx-ljiZx4HFY/edit?ts=5ca4db81
 */

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
        val dd = locationService.list
        dd.onComplete(y ⇒ println(x + "=>" + y))
        dd
        val connection = HttpConnection(ComponentId(s"$x@TestServer", ComponentType.Service))
        locationService.track2(connection).runForeach(dd ⇒ println("+++++++++++++++++++++++++" + dd))
      }
      .onComplete(println)
    Thread.sleep(1000)

//    val connection = Http().cachedHostConnectionPool[Int]("localhost", 7654)
//
//    Source(1 to 64)
//      .map(i => (HttpRequest(uri = Uri("https://localhost:7654/location/list")), i))
//      .via(connection)
//      .runWith(Sink.foreach {
//        case (Success(_), i) => println(s"[${LocalDateTime.now}] $i succeeded")
//        case (Failure(e), i) => println(s"[${LocalDateTime.now}] $i failed: $e")
//      })

  }

}
