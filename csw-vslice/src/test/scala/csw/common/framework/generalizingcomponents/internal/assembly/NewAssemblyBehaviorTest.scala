//package csw.common.framework.generalizingcomponents.internal.assembly
//
//import akka.typed.ActorSystem
//import akka.typed.scaladsl.{Actor, ActorContext}
//import akka.typed.testkit.TestKitSettings
//import akka.typed.testkit.scaladsl.TestProbe
//import akka.util.Timeout
//import csw.common.framework.models.Component.{AssemblyInfo, DoNotRegister}
//import csw.common.framework.models.RunningMsg.DomainMsg
//import csw.services.location.models.ConnectionType.AkkaType
//import org.mockito.Mockito.{verify, when}
//import org.scalatest.mockito.MockitoSugar
//import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
//
//import scala.concurrent.duration.DurationDouble
//import scala.concurrent.{Await, Future}
//
//sealed trait AssemblyDomainMessagesNew extends DomainMsg
//
//class NewAssemblyBehaviorTest extends FunSuite with Matchers with BeforeAndAfterAll with MockitoSugar {
//
//  implicit val system   = ActorSystem("actor-system", Actor.empty)
//  implicit val settings = TestKitSettings(system)
//  implicit val timeout  = Timeout(5.seconds)
//
//  override def afterAll(): Unit = {
//    system.terminate()
//  }
//
//  def getSampleAssemblyFactory(
//      assemblyHandlers: NewAssemblyHandlers[AssemblyDomainMessagesNew]
//  ): AssemblyFactory[AssemblyDomainMessagesNew] =
//    new AssemblyFactory[AssemblyDomainMessagesNew] {
//      override def make(ctx: ActorContext[AssemblyMsgNew],
//                        assemblyInfo: AssemblyInfo): NewAssemblyHandlers[AssemblyDomainMessagesNew] = assemblyHandlers
//    }
//
//  test("Assembly actor sends Initialized and Running message to Supervisor") {
//    val sampleAssemblyHandler = mock[NewAssemblyHandlers[AssemblyDomainMessagesNew]]
//
//    when(sampleAssemblyHandler.initialize()).thenReturn(Future.unit)
//
//    val supervisorProbe = TestProbe[ComponentResponseMode]
//
//    val assemblyInfo = AssemblyInfo("trombone",
//                                    "wfos",
//                                    "csw.common.components.assembly.SampleAssembly",
//                                    DoNotRegister,
//                                    Set(AkkaType),
//                                    Set.empty)
//
//    val assemblyRef =
//      Await.result(
//        system.systemActorOf[Nothing](
//          getSampleAssemblyFactory(sampleAssemblyHandler).behaviour(assemblyInfo, supervisorProbe.ref),
//          "assembly"
//        ),
//        5.seconds
//      )
//
//    val initialized = supervisorProbe.expectMsgType[Initialized]
//    initialized.componentRef shouldBe assemblyRef
//
//    initialized.componentRef ! Run
//
//    val running = supervisorProbe.expectMsgType[Running]
//    verify(sampleAssemblyHandler).onRun()
//    verify(sampleAssemblyHandler).isOnline_=(true)
//
//    running.componentRef shouldBe assemblyRef
//  }
//}
