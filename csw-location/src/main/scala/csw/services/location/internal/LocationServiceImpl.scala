package csw.services.location.internal

import akka.Done
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw.services.location.internal.wrappers.JmDnsApi
import csw.services.location.models._
import csw.services.location.scaladsl.{ActorRuntime, LocationService}

import scala.async.Async._
import scala.concurrent.Future

private[location] class LocationServiceImpl(
  jmDnsApi: JmDnsApi,
  actorRuntime: ActorRuntime,
  locationEventStream: LocationEventStream
) extends LocationService { outer =>

  import actorRuntime._

  override def register(reg: Registration): Future[RegistrationResult] = async {
    await(list).find(_.connection == reg.connection) match {
      case Some(_) => throw new IllegalStateException(s"A service with name ${reg.connection.name} is already registered")
      case None    => await(registerUniqueService(reg))
    }
  }

  override def unregister(connection: Connection): Future[Done] = async {
    val locationF = locationEventStream.broadcast.removed(
      connection,
      jmDnsApi.unregisterService(connection)
    )
    await(locationF)
    Done
  }

  override def unregisterAll(): Future[Done] = Future {
    jmDnsApi.unregisterAllServices()
    Thread.sleep(4000)
    Done
  }(jmDnsDispatcher)

  override def resolve(connection: Connection): Future[Resolved] = async {
    await(list).find(_.connection == connection) match {
      case Some(location: Resolved) => location
      case _                        => await(resolveService(connection))
    }
  }

  override def resolve(connections: Set[Connection]): Future[Set[Resolved]] = {
    Future.traverse(connections)(resolve)
  }

  override def list: Future[List[Location]] = locationEventStream.list

  override def list(componentType: ComponentType): Future[List[Location]] = async {
    await(list).filter(_.connection.componentId.componentType == componentType)
  }

  override def list(hostname: String): Future[List[Resolved]] = async {
    await(list).collect {
      case x: Resolved if x.uri.getHost == hostname => x
    }
  }

  override def list(connectionType: ConnectionType): Future[List[Location]] = async {
    await(list).filter(_.connection.connectionType == connectionType)
  }

  override def track(connection: Connection): Source[Location, KillSwitch] = {
    locationEventStream.broadcast.filter(connection)
  }

  override def shutdown(): Future[Done] = Future {
    jmDnsApi.close()
    Done
  }(jmDnsDispatcher)

  private def registrationResult(connection: Connection) = new RegistrationResult {
    override def componentId: ComponentId = connection.componentId

    override def unregister(): Future[Done] = outer.unregister(connection)
  }

  private def registerUniqueService(reg: Registration) = Future {
    jmDnsApi.registerService(reg)
    registrationResult(reg.connection)
  }(jmDnsDispatcher)

  private def resolveService(connection: Connection) = locationEventStream.broadcast.resolved(
    connection,
    jmDnsApi.requestServiceInfo(connection)
  )
}
