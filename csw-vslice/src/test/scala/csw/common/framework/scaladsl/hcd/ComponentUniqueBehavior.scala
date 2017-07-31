package csw.common.framework.scaladsl.hcd

import akka.typed.ActorSystem
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import csw.common.components.hcd.{AxisStatistics, HcdDomainMessage}
import csw.common.framework.models.{HcdMsg, HcdResponseMode}
import csw.common.framework.models.HcdResponseMode.{Initialized, Running}
import csw.common.framework.models.InitialHcdMsg.Run
import csw.common.framework.models.RunningHcdMsg.DomainHcdMsg
import org.mockito.Mockito.{verify, when}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class ComponentUniqueBehavior extends FunSuite with Matchers with BeforeAndAfterAll with MockitoSugar {

  implicit val system   = ActorSystem("testHcd", Actor.empty)
  implicit val settings = TestKitSettings(system)
  implicit val timeout  = Timeout(5.seconds)

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  def getSampleHcdFactory(hcdHandlers: HcdHandlers[HcdDomainMessage]): HcdHandlersFactory[HcdDomainMessage] =
    new HcdHandlersFactory[HcdDomainMessage] {
      override def make(ctx: ActorContext[HcdMsg]): HcdHandlers[HcdDomainMessage] = hcdHandlers
    }

  test("hcd component should send initialize and running message to supervisor") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMessage]]

    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe: TestProbe[HcdResponseMode] = TestProbe[HcdResponseMode]

    val hcdRef =
      Await.result(
        system.systemActorOf[Nothing](getSampleHcdFactory(sampleHcdHandler).behaviour(supervisorProbe.ref),
          "sampleHcd"),
        5.seconds
      )

    val initialized = supervisorProbe.expectMsgType[Initialized]
    initialized.hcdRef ! Run

    val running = supervisorProbe.expectMsgType[Running]
    val axisStatistics = AxisStatistics(1)
    running.hcdRef ! DomainHcdMsg(axisStatistics)

    verify(sampleHcdHandler).onDomainMsg(AxisStatistics(1))
  }
}
