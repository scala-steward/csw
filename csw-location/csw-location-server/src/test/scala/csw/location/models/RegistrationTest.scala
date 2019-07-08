package csw.location.models

import java.net.URI

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorRefOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.actor.{ActorPath, typed}
import akka.serialization.Serialization
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.api.models._
import csw.location.client.ActorSystemFactory
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.network.utils.Networks
import csw.params.core.models.Prefix
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class RegistrationTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  implicit val actorSystem: typed.ActorSystem[_] = ActorSystemFactory.remote(Behaviors.empty, "my-actor-1")

  test("should able to create the AkkaRegistration which should internally create AkkaLocation") {
    val hostname = Networks().hostname

    val akkaConnection = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
    val actorRef       = actorSystem
    val actorPath      = ActorPath.fromString(Serialization.serializedActorPath(actorRef.toUntyped))
    val akkaUri        = new URI(actorPath.toString)
    val prefix         = Prefix("nfiraos.ncc.trombone")

    val akkaRegistration = AkkaRegistration(akkaConnection, prefix, actorRef)

    val expectedAkkaLocation = AkkaLocation(akkaConnection, prefix, akkaUri, actorRef)

    akkaRegistration.location(hostname) shouldBe expectedAkkaLocation
  }

  test("should able to create the HttpRegistration which should internally create HttpLocation") {
    val hostname = Networks().hostname
    val port     = 9595
    val prefix   = "/trombone/hcd"

    val httpConnection   = HttpConnection(ComponentId("trombone", ComponentType.HCD))
    val httpRegistration = HttpRegistration(httpConnection, port, prefix)

    val expectedhttpLocation = HttpLocation(httpConnection, new URI(s"http://$hostname:$port/$prefix"))

    httpRegistration.location(hostname) shouldBe expectedhttpLocation
  }

  test("should able to create the TcpRegistration which should internally create TcpLocation") {
    val hostname = Networks().hostname
    val port     = 9596

    val tcpConnection   = TcpConnection(ComponentId("lgsTrombone", ComponentType.HCD))
    val tcpRegistration = TcpRegistration(tcpConnection, port)

    val expectedTcpLocation = TcpLocation(tcpConnection, new URI(s"tcp://$hostname:$port"))

    tcpRegistration.location(hostname) shouldBe expectedTcpLocation
  }

  test("should not allow AkkaRegistration using local ActorRef") {
    val config: Config = ConfigFactory.parseString("""
        akka.actor.provider = local
      """)

    implicit val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "local-actor-system", config)
    val actorRef                                         = actorSystem.spawn(Behaviors.empty, "my-actor-2")
    val akkaConnection                                   = AkkaConnection(ComponentId("hcd1", ComponentType.HCD))
    val prefix                                           = Prefix("nfiraos.ncc.trombone")

    intercept[LocalAkkaActorRegistrationNotAllowed] {
      AkkaRegistration(akkaConnection, prefix, actorRef)
    }
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 10.seconds)
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate
    Await.result(actorSystem.whenTerminated, 10.seconds)
    super.afterAll()
  }
}
