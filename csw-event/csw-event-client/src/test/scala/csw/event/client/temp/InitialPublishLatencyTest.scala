package csw.event.client.temp

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import csw.event.client.EventServiceFactory
import csw.event.client.helpers.Utils
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.events.Event
import org.scalatest.FunSuite

class InitialPublishLatencyTest extends FunSuite {

  private implicit val system: ActorSystem    = ActorSystem()
  private implicit val mat: ActorMaterializer = ActorMaterializer()
  private val ls: LocationService             = HttpLocationServiceFactory.makeLocalClient
  private val factory                         = new EventServiceFactory().make(ls)
  private val event                           = Utils.makeEvent(0)
  import factory._

  ignore("should not incurr high latencies for initially published events") {
    defaultSubscriber.subscribeCallback(Set(event.eventKey), report)

    (0 to 50).foreach { id ⇒
      defaultPublisher.publish(Utils.makeEvent(id))
      Thread.sleep(10)
    }
  }

  private def report(event: Event): Unit = {
    val currentTime = System.currentTimeMillis()
    val origTime    = event.eventTime.value.toEpochMilli

    println(currentTime - origTime)
  }
}
