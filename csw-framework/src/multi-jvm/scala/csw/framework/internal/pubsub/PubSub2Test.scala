package csw.framework.internal.pubsub

import akka.actor.ActorSystem
import csw.services.location.helpers.{LSNodeSpec, OneMemberAndSeed}
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Await
import scala.concurrent.duration._

class PubSub2TestMultiJvmNode1 extends PubSub2Test(ignore = 0)
class PubSub2TestMultiJvmNode2 extends PubSub2Test(ignore = 0)

case class TestData(value: Int)

class PubSub2Test(ignore: Int) extends LSNodeSpec(config = new OneMemberAndSeed) with BeforeAndAfterEach {

  import config._
  import cswCluster.mat

  val assemblyActorSystem = ActorSystem("assembly-actor-system")

  override protected def afterEach(): Unit =
    Await.result(locationService.unregisterAll(), 10.seconds)

  override def afterAll(): Unit = {
    super.afterAll()
    assemblyActorSystem.terminate()
  }

  test("See if pubsub 2 function is supported") {

/*
    runOn(seed) {

      val pubsub = PubSubBehavior2.behavior[TestData]()

      val pub = spawn(pubb)
    }
    */
  }

}
