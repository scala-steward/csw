package csw.Supervision

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SupervisionTest extends FunSuite with Matchers {

  implicit val system   = ActorSystem("testHcd", Actor.empty)
  implicit val settings = TestKitSettings(system)
  implicit val timeout  = Timeout(5.seconds)

  test("abc") {
    val testProbe = TestProbe[FromActorMsg]

    val parent = Await.result(system.systemActorOf(Parent.behavior(), "Parent"), 5.seconds)

    parent ! Spawn(testProbe.ref)
    val spawnedActor = testProbe.expectMsgType[Spawned]

    spawnedActor.ref ! Cry
    testProbe.expectMsgType[Stopped]
  }
}
