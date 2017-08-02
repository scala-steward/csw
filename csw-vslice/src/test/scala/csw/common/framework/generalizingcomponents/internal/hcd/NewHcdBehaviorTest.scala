//package csw.common.framework.generalizingcomponents.internal.hcd
//
//import akka.typed.ActorSystem
//import akka.typed.scaladsl.{Actor, ActorContext}
//import akka.typed.testkit.TestKitSettings
//import akka.typed.testkit.scaladsl.TestProbe
//import akka.util.Timeout
//import csw.common.framework.generalizingcomponents.ComponentResponseMode.{Initialized, Running}
//import csw.common.framework.generalizingcomponents.InitialComponentMsg.Run
//import csw.common.framework.generalizingcomponents.{ComponentResponseMode, DomainMsgNew, HcdMsgNew}
//import csw.common.framework.generalizingcomponents.api.hcd.{HcdFactory, NewHcdHandlers}
//import org.mockito.Mockito.{verify, when}
//import org.scalatest.mockito.MockitoSugar
//import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
//
//import scala.concurrent.duration.DurationDouble
//import scala.concurrent.{Await, Future}
//
//class NewHcdBehaviorTest extends FunSuite with Matchers with BeforeAndAfterAll with MockitoSugar {
//
//  implicit val system   = ActorSystem("testHcd", Actor.empty)
//  implicit val settings = TestKitSettings(system)
//  implicit val timeout  = Timeout(5.seconds)
//
//  override protected def afterAll(): Unit = {
//    system.terminate()
//  }
//
//  def getSampleHcdFactory(hcdHandlers: NewHcdHandlers[HcdDomainMessageNew]): HcdFactory[HcdDomainMessageNew] =
//    new HcdFactory[HcdDomainMessageNew] {
//      override def make(ctx: ActorContext[HcdMsgNew]): NewHcdHandlers[HcdDomainMessageNew] = hcdHandlers
//    }
//
//  test("hcd component should send initialize and running message to supervisor") {
//    val sampleHcdHandler = mock[NewHcdHandlers[HcdDomainMessageNew]]
//
//    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)
//
//    val supervisorProbe: TestProbe[ComponentResponseMode] = TestProbe[ComponentResponseMode]
//
//    val hcdRef =
//      Await.result(
//        system.systemActorOf[Nothing](getSampleHcdFactory(sampleHcdHandler).behaviour(supervisorProbe.ref),
//                                      "sampleHcd"),
//        5.seconds
//      )
//
//    val initialized = supervisorProbe.expectMsgType[Initialized]
//
//    verify(sampleHcdHandler).initialize()
//    initialized.componentRef shouldBe hcdRef
//
//    initialized.componentRef ! Run
//
//    val running = supervisorProbe.expectMsgType[Running]
//
//    verify(sampleHcdHandler).onRun()
//    verify(sampleHcdHandler).isOnline_=(true)
//
//    running.componentRef shouldBe hcdRef
//  }
//}
//
//sealed trait HcdDomainMessageNew extends DomainMsgNew
