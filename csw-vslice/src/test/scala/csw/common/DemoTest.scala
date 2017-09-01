//package csw.common
//
//import akka.actor.ActorSystem
//import akka.serialization.SerializationExtension
//import akka.typed.scaladsl.adapter.UntypedActorSystemOps
//import akka.typed.{ActorRef, Behavior}
//import com.twitter.chill.akka.AkkaSerializer
//import csw.param.ParamSerializable
//import csw.services.location.models.ServiceRef
//import org.scalatest.{FunSuite, Matchers}
//
//import scala.reflect.ClassTag
//
//class DemoTest extends FunSuite with Matchers {
//  private val system              = ActorSystem("aa")
//  private final val serialization = SerializationExtension(system)
//
//  private val ref: ActorRef[Int] = system.spawn(Behavior.empty[Int], "anc")
//
//  test("should serialize Setup") {
//    val setup           = ServiceRef(ref)
//    val setupSerializer = serialization.findSerializerFor(setup)
//
//    setupSerializer.getClass shouldBe classOf[AkkaSerializer]
//
//    val setupBytes: Array[Byte] = setupSerializer.toBinary(setup)
//    val setup2                  = setupSerializer.fromBinary(setupBytes).asInstanceOf[ServiceRef[_]]
//
//    setup2 shouldBe setup
//    setup2.typedRef[String] shouldBe ref
////    setup2.actorRef shouldBe ref
//  }
//}
