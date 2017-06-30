package csw.apps.clusterseed.admin.http

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import csw.apps.clusterseed.admin.internal.AdminWiring
import org.scalatest.FunSuite

class WebsocketRoutesTest extends FunSuite with ScalatestRouteTest {

  private val wiring = new AdminWiring

  test("dummy") {
    val wsClient = WSProbe()

    WS("/greeter", wsClient.flow) ~> wiring.adminRoutes.route ~> check {
      println(isWebSocketUpgrade)

      wsClient.sendMessage("""val who1 = "world"""")
//      wsClient.expectMessage("""val who1 = "world"""")
      println(wsClient.expectMessage().asTextMessage.getStrictText)

      wsClient.sendMessage("""val who = "world"""")
      println(wsClient.expectMessage().asTextMessage.getStrictText)
//
      wsClient.sendMessage("""println(("hello", who))""")
      println(wsClient.expectMessage().asTextMessage.getStrictText)
    }
  }
}
