package csw.services.location.scaladsl

import com.typesafe.config.{Config, ConfigFactory}
import csw.services.location.common.Networks

import scala.collection.JavaConverters.{asJavaCollectionConverter, mapAsJavaMapConverter}

class ConfigData(name: String) {
  private val hostname = Networks.getPrimaryIpv4Address.getHostAddress
  private val port = sys.props.getOrElse("akkaPort", "2552")
  private val seedHost = sys.props.getOrElse("akkaSeed", hostname)
  private val seedNode = s"akka.tcp://$name@$seedHost:2552"

  def config(extraSettings: Map[String, Any]): Config = {
    val settings: Map[String, Any] = Map(
      "akka.remote.netty.tcp.hostname" -> hostname,
      "akka.remote.netty.tcp.port" -> port,
      "akka.cluster.seed-nodes" -> List(seedNode).asJavaCollection
    ) ++ extraSettings

    ConfigFactory.parseMap(settings.asJava).withFallback(ConfigFactory.load())
  }
}
