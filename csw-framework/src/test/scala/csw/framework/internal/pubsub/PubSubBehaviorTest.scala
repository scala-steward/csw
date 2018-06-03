package csw.framework.internal.pubsub

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.typed.scaladsl.MutableBehavior
import akka.actor.{typed, ActorSystem}
import akka.testkit.typed.TestKitSettings
import akka.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import csw.framework.FrameworkTestMocks
import csw.messages.framework
import csw.messages.framework.PubSub.{Publish, Subscribe, SubscribeOnly, Unsubscribe}
import csw.messages.framework.{LifecycleStateChanged, PubSub, SupervisorLifecycleState}
import csw.messages.scaladsl.ComponentMessage
import csw.services.location.commons.ActorSystemFactory
import csw.services.logging.scaladsl.Logger
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PubSubBehaviorTest extends FunSuite with Matchers with BeforeAndAfterAll {

  trait MutableActorMock[T] { this: MutableBehavior[T] ⇒
    protected lazy val log: Logger = MockitoSugar.mock[Logger]
  }

  implicit val untypedSystem: ActorSystem       = ActorSystemFactory.remote("test-1")
  implicit val system: typed.ActorSystem[_]     = untypedSystem.toTyped
  implicit val testKitSettings: TestKitSettings = TestKitSettings(system)

  private val mocks = new FrameworkTestMocks()

  private val lifecycleProbe1 = TestProbe[LifecycleStateChanged]
  private val lifecycleProbe2 = TestProbe[LifecycleStateChanged]

  def createPubSubBehavior(): BehaviorTestKit[PubSub[LifecycleStateChanged]] =
    BehaviorTestKit(PubSubBehavior.behavior(mocks.loggerFactory))

  override protected def afterAll(): Unit = Await.result(untypedSystem.terminate(), 5.seconds)

  test("message should be published to all the subscribers") {
    val pubSubBehavior: BehaviorTestKit[PubSub[LifecycleStateChanged]] = createPubSubBehavior()
    val supervisorProbe                                                = TestProbe[ComponentMessage]

    pubSubBehavior.run(Subscribe(lifecycleProbe1.ref))
    pubSubBehavior.run(Subscribe(lifecycleProbe2.ref))

    pubSubBehavior.run(Publish(LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running)))

    lifecycleProbe1.expectMessage(framework.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    lifecycleProbe2.expectMessage(framework.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
  }

  test("message should be published to some subscribers using filter") {
    case class TestData(data: Int)

    val testDataProbe1 = TestProbe[TestData]
    val testDataProbe2 = TestProbe[TestData]

    val pubSubBehavior: BehaviorTestKit[PubSub[TestData]] = BehaviorTestKit(PubSubBehavior.behavior(mocks.loggerFactory))

    pubSubBehavior.run(SubscribeOnly(testDataProbe1.ref, (x: TestData) => x.data == 3))
    pubSubBehavior.run(Subscribe(testDataProbe2.ref))

    pubSubBehavior.run(Publish(TestData(3)))

    testDataProbe1.expectMessage(TestData(3))
    testDataProbe2.expectMessage(TestData(3))

    pubSubBehavior.run(Publish(TestData(1)))
    testDataProbe2.expectMessage(TestData(1))
    testDataProbe1.expectNoMessage(50.millis)
  }

  test("should not receive messages on un-subscription") {
    val pubSubBehavior: BehaviorTestKit[PubSub[LifecycleStateChanged]] = createPubSubBehavior()
    val supervisorProbe                                                = TestProbe[ComponentMessage]

    pubSubBehavior.run(Subscribe(lifecycleProbe1.ref))
    pubSubBehavior.run(Subscribe(lifecycleProbe2.ref))
    pubSubBehavior.run(Unsubscribe(lifecycleProbe1.ref))

    pubSubBehavior.run(
      Publish(framework.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    )

    lifecycleProbe2.expectMessage(framework.LifecycleStateChanged(supervisorProbe.ref, SupervisorLifecycleState.Running))
    lifecycleProbe1.expectNoMessage(50.millis)
  }

}
