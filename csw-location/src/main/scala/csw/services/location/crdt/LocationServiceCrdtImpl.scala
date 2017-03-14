package csw.services.location.crdt

import akka.Done
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata.{DistributedData, LWWRegister, LWWRegisterKey}
import akka.pattern.ask
import akka.util.Timeout
import csw.services.location.scaladsl.ActorRuntime
import csw.services.location.scaladsl.models._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class LocationServiceCrdtImpl(actorRuntime: ActorRuntime) { outer =>

  import actorRuntime._

  private val replicator = DistributedData(actorSystem).replicator
  implicit val timeout = Timeout(10.seconds)

  def register(location: Location): Future[RegistrationResult] = {
    val key = LWWRegisterKey[Location](location.connection.name)
    val update = Update(key, LWWRegister(location), WriteLocal)(_.withValue(location))
    (replicator ? update).flatMap {
      case x: UpdateSuccess[_] => Future.successful(registrationResult(location))
      case x: UpdateFailure[_] => Future.failed(new RuntimeException(s"unable to register ${location.connection}"))
    }
  }

  def unregister(location: Location): Future[Done] = {
    val key = LWWRegisterKey[Location](location.connection.name)
    val delete = Delete(key, WriteLocal)
    (replicator ? delete).flatMap {
      case x: DeleteSuccess[_] => Future.successful(Done)
      case _                   => Future.failed(new RuntimeException(s"unable to unregister ${location.connection}"))
    }
  }

  def resolve(connection: Connection): Future[Location] = {
    val key = LWWRegisterKey[Location](connection.name)
    val get = Get(key, ReadLocal)
    (replicator ? get).flatMap {
      case x: GetSuccess[Location] => Future.successful(x.get(key).value)
      case _                       => Future.failed(new RuntimeException(s"unable to find $connection"))
    }
  }

  private def registrationResult(location: Location): RegistrationResult = new RegistrationResult {
    override def componentId: ComponentId = location.connection.componentId

    override def unregister(): Future[Done] = outer.unregister(location)
  }

}
