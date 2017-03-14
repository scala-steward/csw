package csw.services.location.scaladsl

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import csw.services.location.common.Networks

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

class ActorRuntime(name: String, _settings: Map[String, Any] = Map.empty) {

  private val hostname = Networks.getPrimaryIpv4Address.getHostAddress
  private val port = sys.props.getOrElse("akka.port", "2552")
  private val seedHost = sys.props.getOrElse("akka.seed", hostname)
  private val seedNode = s"akka.tcp://$name@$seedHost:2552"

  val config: Config = {
    val settings: Map[String, Any] = Map(
      "akka.remote.netty.tcp.hostname" -> hostname,
      "akka.remote.netty.tcp.port" -> port,
      "akka.cluster.seed-nodes" -> List(seedNode).asJava
    ) ++ _settings

    ConfigFactory.parseMap(settings.asJava).withFallback(ConfigFactory.load())
  }



  implicit val actorSystem: ActorSystem = ActorSystem(name, config)
  implicit val ec: ExecutionContext = actorSystem.dispatcher
  implicit val mat: Materializer = makeMat()

  def makeMat(): Materializer = ActorMaterializer()
}
