package csw.framework.internal.pubsub

import akka.actor.Scheduler
import akka.stream.{ActorMaterializer, Materializer}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.testkit.typed.TestKitSettings
import akka.testkit.typed.scaladsl.TestProbe
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.messages.commands.CommandResponse.Accepted
import csw.messages.commands.Setup
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{AkkaLocation, ComponentId, ComponentType}
import csw.messages.params.models.ObsId
import csw.messages.params.states.CurrentState
import csw.services.command.scaladsl.CommandService
import csw.services.location.helpers.{LSNodeSpec, OneMemberAndSeed}
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContextExecutor}

class PubSubRemoteMultiJvmNode1 extends PubSubRemote(ignore = 0)

class PubSubRemoteMultiJvmNode2 extends PubSubRemote(ignore = 0)

class PubSubRemote(ignore: Int) extends LSNodeSpec(config = new OneMemberAndSeed) with ScalaFutures {
  import csw.common.components.pubsub.ComponentStateForPubSub._

  import config._

  implicit val actorSystem: ActorSystem[_] = system.toTyped
  implicit val mat: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = actorSystem.executionContext
  implicit val timeout: Timeout = 20.seconds
  implicit val scheduler: Scheduler = actorSystem.scheduler
  implicit val testkit: TestKitSettings = TestKitSettings(actorSystem)

  private val probe = TestProbe[CurrentState]
  // A second probe that will handle only csprefix1
  private val probe2 = TestProbe[CurrentState]

  test("See if pubsub 2 function is supported") {

    runOn(seed) {
      // cluster seed is running on jvm-1
      enterBarrier("spawned")
      val obsId = ObsId("Obs001")

      // resolve assembly running in jvm-2 and send setup command expecting immediate command completion response
      val hcdLocF =
        locationService.resolve(
          AkkaConnection(ComponentId("Test_Remote_PubSub_HCD", ComponentType.HCD)),
          5.seconds
        )
      val hcdLocation: AkkaLocation = Await.result(hcdLocF, 10.seconds).get
      val hcdCommandService = new CommandService(hcdLocation)

      enterBarrier("pubsub1")

      val setup = Setup(prefix, publishCmd, Some(obsId))

      // Subscribe probe1 to all CurrentState values
      hcdCommandService.subscribeCurrentState(probe.ref ! _)

      val response1 = hcdCommandService.submit(setup)
      whenReady(response1, PatienceConfiguration.Timeout(10.seconds)) { result =>
        result shouldBe a[Accepted]
      }

      probe.expectMessage(CurrentState(prefix, stateName1))
      probe.expectMessage(CurrentState(prefix, stateName2))

      enterBarrier("pubsub2")

      // Subscribe probe2 to only csprefix1
      val cssubscriber = hcdCommandService.subscribeCurrentState(probe2.ref ! _, Seq(stateName1))

      val response2 = hcdCommandService.submit(setup)
      whenReady(response2, PatienceConfiguration.Timeout(10.seconds)) { result =>
        result shouldBe a[Accepted]
      }

      probe.expectMessage(CurrentState(prefix, stateName1))
      probe.expectMessage(CurrentState(prefix, stateName2))
      probe.expectNoMessage(200.millis)

      probe2.expectMessage(CurrentState(prefix, stateName1))
      probe2.expectNoMessage(200.millis)

      enterBarrier("pubsub3")

      // One more fun test to see if unsubscribe works
      cssubscriber.unsubscribe()

      val response3 = hcdCommandService.submit(setup)
      whenReady(response3, PatienceConfiguration.Timeout(10.seconds)) { result =>
        result shouldBe a[Accepted]
      }

      probe.expectMessage(CurrentState(prefix, stateName1))
      probe.expectMessage(CurrentState(prefix, stateName2))
      probe.expectNoMessage(200.millis)

      probe2.expectNoMessage(200.millis)
    }

    runOn(member) {
      // spawn single hcd running in Standalone mode in jvm-3
      val wiring = FrameworkWiring.make(system, locationService)
      val hcdConf = ConfigFactory.load("pubsub/pubsub_hcd.conf")
      Await.result(Standalone.spawn(hcdConf, wiring), 5.seconds)
      enterBarrier("spawned")
      enterBarrier("pubsub1")
      enterBarrier("pubsub2")
      enterBarrier("pubsub3")

    }
    enterBarrier("end")

  }

}
