package csw.location.server.internal

import java.net.URI

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.serialization.SerializationExtension
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.api.models._
import csw.params.core.models.Prefix
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

private[location] class LocationAkkaSerializerTest extends FunSuite with Matchers with BeforeAndAfterAll {

  // need to instantiate from remote factory to wire up serializer
  private final val system        = ActorSystem(Behaviors.empty, "example")
  private final val serialization = SerializationExtension(system.toUntyped)
  private final val prefix        = Prefix("wfos.prog.cloudcover")

  override protected def afterAll(): Unit = {
    system.terminate()
    Await.result(system.whenTerminated, 2.seconds)
  }

  test("should use location serializer for Connection (de)serialization") {
    val testData = Table(
      "Connection models",
      AkkaConnection(ComponentId("TromboneAssembly", Assembly)),
      HttpConnection(ComponentId("TromboneAssembly", Assembly)),
      TcpConnection(ComponentId("TromboneAssembly", Assembly))
    )

    forAll(testData) { connection ⇒
      val serializer = serialization.findSerializerFor(connection)
      serializer.getClass shouldBe classOf[LocationAkkaSerializer]

      val bytes = serializer.toBinary(connection)
      serializer.fromBinary(bytes, Some(connection.getClass)) shouldEqual connection
    }
  }

  test("should use location serializer for Location (de)serialization") {
    val akkaConnection = AkkaConnection(ComponentId("TromboneAssembly", Assembly))
    val httpConnection = HttpConnection(ComponentId("TromboneAssembly", Assembly))
    val tcpConnection  = TcpConnection(ComponentId("TromboneAssembly", Assembly))
    val testData = Table(
      "Location models",
      AkkaLocation(akkaConnection, prefix, new URI(""), system),
      HttpLocation(httpConnection, new URI("")),
      TcpLocation(tcpConnection, new URI(""))
    )

    forAll(testData) { location ⇒
      val serializer = serialization.findSerializerFor(location)
      serializer.getClass shouldBe classOf[LocationAkkaSerializer]

      val bytes = serializer.toBinary(location)
      serializer.fromBinary(bytes, Some(location.getClass)) shouldEqual location
    }
  }

  test("should use location serializer for Registration (de)serialization") {
    val akkaConnection = AkkaConnection(ComponentId("TromboneAssembly", Assembly))
    val httpConnection = HttpConnection(ComponentId("TromboneAssembly", Assembly))
    val tcpConnection  = TcpConnection(ComponentId("TromboneAssembly", Assembly))
    val testData = Table(
      "Registration models",
      AkkaRegistration(akkaConnection, prefix, system),
      HttpRegistration(httpConnection, 1234, ""),
      TcpRegistration(tcpConnection, 1234)
    )

    forAll(testData) { registration ⇒
      val serializer = serialization.findSerializerFor(registration)
      serializer.getClass shouldBe classOf[LocationAkkaSerializer]

      val bytes = serializer.toBinary(registration)
      serializer.fromBinary(bytes, Some(registration.getClass)) shouldEqual registration
    }
  }

  test("should use location serializer for TrackingEvent (de)serialization") {
    val akkaConnection = AkkaConnection(ComponentId("TromboneAssembly", Assembly))
    val akkaLocation   = AkkaLocation(akkaConnection, prefix, new URI(""), system)

    val testData = Table(
      "TrackingEvent models",
      LocationUpdated(akkaLocation),
      LocationRemoved(akkaConnection)
    )

    forAll(testData) { trackingEvent ⇒
      val serializer = serialization.findSerializerFor(trackingEvent)
      serializer.getClass shouldBe classOf[LocationAkkaSerializer]

      val bytes = serializer.toBinary(trackingEvent)
      serializer.fromBinary(bytes, Some(trackingEvent.getClass)) shouldEqual trackingEvent
    }
  }
}
