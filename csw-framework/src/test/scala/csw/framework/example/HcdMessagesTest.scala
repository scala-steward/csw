package csw.framework.example

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import org.scalatest.FunSuite

class HcdMessagesTest extends FunSuite {

  val beh: Actor.Immutable[Int] = Actor.immutable[Int] { (ctx, msg) ⇒
    Thread.sleep(500)
    println("inside")
    msg match {
      case x ⇒ println(x); Actor.same
    }
  }

  test("demo") {
    val hey = ActorSystem("hey", beh)
    println("returns")
    hey ! 100
    Thread.sleep(1000)
  }

}
