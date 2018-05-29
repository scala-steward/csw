package csw.framework.internal.pubsub

import akka.actor.Scheduler
import akka.stream.{ActorMaterializer, Materializer}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.testkit.typed.TestKitSettings
import akka.testkit.typed.scaladsl.TestProbe
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.common.components.command.ComponentStateForCommand._
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.messages.commands.CommandResponse.{Accepted, Completed, Invalid}
import csw.messages.commands.{CommandResponse, Setup}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{AkkaLocation, ComponentId, ComponentType}
import csw.messages.params.models.ObsId
import csw.messages.params.states.CurrentState
import csw.services.command.scaladsl.{CommandDistributor, CommandService}
import csw.services.location.helpers.{LSNodeSpec, OneMemberAndSeed}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

class PubSubRemoteMultiJvmNode1 extends PubSubRemote(ignore = 0)
class PubSubRemoteMultiJvmNode2 extends PubSubRemote(ignore = 0)

class PubSubRemote(ignore: Int) extends LSNodeSpec(config = new OneMemberAndSeed) with BeforeAndAfterEach {
  import config._

  implicit val actorSystem: ActorSystem[_]  = system.toTyped
  implicit val mat: Materializer            = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = actorSystem.executionContext
  implicit val timeout: Timeout             = 20.seconds
  implicit val scheduler: Scheduler         = actorSystem.scheduler
  implicit val testkit: TestKitSettings     = TestKitSettings(actorSystem)

  test("See if pubsub 2 function is supported") {

    runOn(seed) {
      // cluster seed is running on jvm-1
      enterBarrier("spawned")
      val obsId = ObsId("Obs001")

      // resolve assembly running in jvm-2 and send setup command expecting immediate command completion response
      val assemblyLocF =
        locationService.resolve(
          AkkaConnection(ComponentId("Test_Component_Running_Long_Command", ComponentType.Assembly)),
          5.seconds
        )
      val assemblyLocation: AkkaLocation = Await.result(assemblyLocF, 10.seconds).get
      val assemblyCommandService         = new CommandService(assemblyLocation)

      val setup = Setup(prefix, longRunning, Some(obsId))
      val probe = TestProbe[CurrentState]

    }

    runOn(member) {
      // spawn single hcd running in Standalone mode in jvm-3
      val wiring  = FrameworkWiring.make(system, locationService)
      val hcdConf = ConfigFactory.load("command/mcs_hcd.conf")
      Await.result(Standalone.spawn(hcdConf, wiring), 5.seconds)
      enterBarrier("spawned")
      enterBarrier("long-commands")
      enterBarrier("multiple-components-submit-multiple-commands")
      enterBarrier("multiple-components-submit-subscribe-multiple-commands")
    }
    enterBarrier("end")

  }

}
