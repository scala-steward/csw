package csw.services.location.crdt

import java.net.URI

import csw.services.location.common.Networks
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.scaladsl.ActorRuntime
import csw.services.location.scaladsl.models.Connection.TcpConnection
import csw.services.location.scaladsl.models.{ComponentId, ComponentType, ResolvedTcpLocation}
import org.scalatest.{FunSuite, Matchers}

class LocationServiceCrdtImplTest extends FunSuite with Matchers {

  test("dd") {
    val actorRuntime = new ActorRuntime("test")
    val crdtImpl = new LocationServiceCrdtImpl(actorRuntime)

    val Port = 1234
    val componentId = ComponentId("redis1", ComponentType.Service)
    val connection = TcpConnection(componentId)
    val uri = new URI(s"tcp://${Networks.getPrimaryIpv4Address.getHostAddress}:$Port")
    val location = ResolvedTcpLocation(connection, uri)

    val result = crdtImpl.register(location).await

    crdtImpl.resolve(connection).await shouldBe location

    result.unregister().await

    intercept[RuntimeException] {
      crdtImpl.resolve(connection).await
    }
  }

}
