package csw.framework.internal.pubsub

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.{typed, ActorSystem}
import akka.actor.typed.scaladsl.MutableBehavior
import akka.testkit.typed.TestKitSettings
import akka.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import csw.framework.FrameworkTestMocks
import csw.messages.framework.CurrentStatePubSub
import csw.messages.framework.CurrentStatePubSub.{Publish, Subscribe, SubscribeOnly, Unsubscribe}
import csw.messages.params.models.Prefix
import csw.messages.params.states.{CurrentState, StateName}
import csw.services.location.commons.ActorSystemFactory
import csw.services.logging.scaladsl.Logger
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Await
import scala.concurrent.duration._

class CurrentStatePubSubTest extends FunSuite with Matchers with BeforeAndAfterAll {

  trait MutableActorMock[T] { this: MutableBehavior[T] ⇒
    protected lazy val log: Logger = MockitoSugar.mock[Logger]
  }

  implicit val untypedSystem: ActorSystem       = ActorSystemFactory.remote("test-1")
  implicit val system: typed.ActorSystem[_]     = untypedSystem.toTyped
  implicit val testKitSettings: TestKitSettings = TestKitSettings(system)

  private val mocks = new FrameworkTestMocks()

  private val currentStateProbe1 = TestProbe[CurrentState]
  private val currentStateProbe2 = TestProbe[CurrentState]

  private val prefix  = Prefix("test.sys")
  private val cs1Name = StateName("cs1")
  private val cs2Name = StateName("cs2")
  private val cs1     = CurrentState(prefix, cs1Name)
  private val cs2     = CurrentState(prefix, cs2Name)

  def createPubSubBehavior(): BehaviorTestKit[CurrentStatePubSub] =
    BehaviorTestKit(CurrentStatePubSubBehavior.behavior(mocks.loggerFactory))

  override protected def afterAll(): Unit = Await.result(untypedSystem.terminate(), 5.seconds)

  test("message should be published to all the subscribers") {
    val pubSubBehavior: BehaviorTestKit[CurrentStatePubSub] = createPubSubBehavior()

    pubSubBehavior.run(Subscribe(currentStateProbe1.ref))
    pubSubBehavior.run(Subscribe(currentStateProbe2.ref))

    pubSubBehavior.run(Publish(cs1))
    pubSubBehavior.run(Publish(cs2))

    currentStateProbe1.expectMessage(cs1)
    currentStateProbe2.expectMessage(cs1)

    currentStateProbe1.expectMessage(cs2)
    currentStateProbe2.expectMessage(cs2)
  }

  test("message should be published to some subscribers using filter") {

    val pubSubBehavior: BehaviorTestKit[CurrentStatePubSub] = createPubSubBehavior()

    pubSubBehavior.run(SubscribeOnly(currentStateProbe1.ref, Seq(cs1Name)))
    pubSubBehavior.run(Subscribe(currentStateProbe2.ref))

    pubSubBehavior.run(Publish(cs1))

    currentStateProbe1.expectMessage(cs1)
    currentStateProbe2.expectMessage(cs1)

    pubSubBehavior.run(Publish(cs2))

    currentStateProbe2.expectMessage(cs2)
    currentStateProbe1.expectNoMessage(50.millis)
  }

  test("should not receive messages on un-subscription") {
    val pubSubBehavior: BehaviorTestKit[CurrentStatePubSub] = createPubSubBehavior()

    pubSubBehavior.run(Subscribe(currentStateProbe1.ref))
    pubSubBehavior.run(Subscribe(currentStateProbe2.ref))
    pubSubBehavior.run(Unsubscribe(currentStateProbe1.ref))

    pubSubBehavior.run(Publish(cs1))

    currentStateProbe2.expectMessage(cs1)
    currentStateProbe1.expectNoMessage(50.millis)
  }

}
