package csw.services.event.apps

import akka.Done
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Keep, Source}
import csw.messages.events.{Event, EventName, SystemEvent}
import csw.services.event.apps.EventUtils._
import csw.services.event.internal.wiring.Wiring
import csw.services.event.scaladsl.{EventPublisher, RedisFactory}
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

object Producer2 extends App with MockitoSugar {

  private val host = "localhost"
  private val port = 6378

  private val warmupMsgs           = 5000
  private val totalTestMsgs        = 200000
  private val totalMessages        = totalTestMsgs + warmupMsgs + 1 //inclusive of end-event
  private val payload: Array[Byte] = ("0" * 100).getBytes("utf-8")

  implicit val system: ActorSystem = ActorSystem("producer")
  private val wiring               = new Wiring(system)
  import wiring._

  private val redisFactory: RedisFactory = new RedisFactory(RedisClient.create(), mock[LocationService], wiring)
  private val publisher: EventPublisher  = redisFactory.publisher(host, port)

  private var counter                  = 0
  private var cancellable: Cancellable = _

  private def eventGenerator(id: Int): Event = {
    counter += 1
    if (counter > totalMessages) cancellable.cancel()
    if (counter < totalMessages) event(EventName(s"$testEventS-$id"), counter, payload)
    else event(EventName(s"${EventUtils.endEventS}-$id"))
  }

  private def source(eventName: EventName): Source[SystemEvent, Future[Done]] =
    Source(1L to totalMessages)
      .throttle(1000, 1.second, 1000, ThrottleMode.shaping)
      .map(id ⇒ event(eventName, id, payload))
      .watchTermination()(Keep.right)

  def startPublishingWithEventGenerator(id: Int): Unit = { cancellable = publisher.publish(eventGenerator(id), 1.millis) }

  def startPublishingWithSource(id: Int): Future[Done] =
    for {
      _   ← publisher.publish(source(EventName(s"$testEventS-$id")))
      end ← publisher.publish(event(EventName(s"$endEventS-$id")))
    } yield end

  def publish(): immutable.Seq[Future[Done]] =
    (1 to 40).map { n ⇒
      startPublishingWithSource(n)
    }

  publish()
}
