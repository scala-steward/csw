package csw.services.location.crdt

import akka.Done
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata._
import akka.pattern.ask
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.services.location.common.Constants
import csw.services.location.scaladsl.ActorRuntime
import csw.services.location.scaladsl.models._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import async.Async._

class LocationServiceCrdtImpl(actorRuntime: ActorRuntime) { outer =>

  import actorRuntime._

  implicit val timeout = Timeout(10.seconds)
  private val registryKey = ORSetKey[ServiceLocation](Constants.RegistryKey)

  def register(location: ServiceLocation): Future[RegistrationResult] = {
    val key = LWWRegisterKey[ServiceLocation](location.connection.name)
    val updateValue = Update(key, LWWRegister(location), WriteLocal)(_.withValue(location))
    val updateRegistry = Update(registryKey, ORSet.empty[ServiceLocation], WriteLocal)(_ + location)
    (replicator ? updateValue).flatMap {
      case _: UpdateSuccess[_] => (replicator ? updateRegistry).map {
        case _: UpdateSuccess[_] => registrationResult(location)
        case x: UpdateFailure[_] => throw new RuntimeException(s"unable to register ${location.connection}")
      }
      case x: UpdateFailure[_] => Future.failed(new RuntimeException(s"unable to register ${location.connection}"))
    }
  }

  def unregister(location: ServiceLocation): Future[Done] = {
    val key = LWWRegisterKey[ServiceLocation](location.connection.name)
    val deleteValue = Delete(key, WriteLocal)
    val deleteFromRegistry = Update(registryKey, ORSet.empty[ServiceLocation], WriteLocal)(_ - location)
    (replicator ? deleteValue).flatMap {
      case x: DeleteSuccess[_] => (replicator ? deleteFromRegistry).map {
        case _: UpdateSuccess[_] => Done
        case _                   => throw new RuntimeException(s"unable to unregister ${location.connection}")
      }
      case _                   => throw new RuntimeException(s"unable to unregister ${location.connection}")
    }
  }

  def unregisterAll(): Future[Done] = async {
    val locations = await(list)
    await(Future.traverse(locations)(unregister))
    Done
  }


  def resolve(connection: Connection): Future[ServiceLocation] = {
    val key = LWWRegisterKey[ServiceLocation](connection.name)
    val get = Get(key, ReadLocal)
    (replicator ? get).map {
      case x@GetSuccess(`key`, _) => x.get(key).value
      case _                      => throw new RuntimeException(s"unable to find $connection")
    }
  }

  def resolve(connections: Set[Connection]): Future[Set[ServiceLocation]] =
    Future.traverse(connections)(resolve)

  def list: Future[List[ServiceLocation]] = {
    val get = Get(registryKey, ReadLocal)
    (replicator ? get).map {
      case x@GetSuccess(`registryKey`, _) => x.get(registryKey).elements.toList
      case _                              => throw new RuntimeException(s"unable to get the list of registered locations")
    }
  }

  def list(componentType: ComponentType): Future[List[ServiceLocation]] = async {
    await(list).filter(_.connection.componentId.componentType == componentType)
  }

  def list(hostname: String): Future[List[ServiceLocation]] = async {
    await(list).filter(_.uri.getHost == hostname)
  }

  def list(connectionType: ConnectionType): Future[List[ServiceLocation]] = async {
    await(list).filter(_.connection.connectionType == connectionType)
  }

  def track(connection: Connection): Source[ServiceLocation, KillSwitch] = {
    val key = LWWRegisterKey[ServiceLocation](connection.name)

    Subscribe(key, null)
    ???
  }

  private def registrationResult(location: ServiceLocation): RegistrationResult = new RegistrationResult {
    override def componentId: ComponentId = location.connection.componentId

    override def unregister(): Future[Done] = outer.unregister(location)
  }

}
