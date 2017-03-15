package csw.services.location.scaladsl

import akka.actor.{ActorRef, ActorSystem, Terminated}
import akka.cluster.Cluster
import akka.cluster.ddata.DistributedData
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

class ActorRuntime(name: String, config: Config) {

  def this(name: String) = this(name, ConfigData.config(Map.empty))

  implicit val actorSystem: ActorSystem = ActorSystem(name, config)
  implicit val ec: ExecutionContext = actorSystem.dispatcher
  implicit val mat: Materializer = makeMat()
  implicit val cluster: Cluster = Cluster(actorSystem)
  val replicator: ActorRef = DistributedData(actorSystem).replicator

  def makeMat(): Materializer = ActorMaterializer()
  def terminate(): Future[Terminated] = actorSystem.terminate()
}
