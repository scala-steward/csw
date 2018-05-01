package csw.services.event.apps

import java.time.Instant
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.{ExecutorService, Executors}

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink}
import csw.messages.events.{EventKey, EventName, SystemEvent}
import csw.services.event.apps.EventUtils._
import csw.services.event.internal.wiring.Wiring
import csw.services.event.scaladsl.{EventSubscriber, RedisFactory}
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import org.HdrHistogram.Histogram
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}

object Consumer2 extends App with MockitoSugar {

  private val host = "localhost"
  private val port = 6378

  private val warmupMsgs = 5000

  implicit val system: ActorSystem = ActorSystem("producer")
  private val wiring               = new Wiring(system)
  import wiring._

  private val redisFactory: RedisFactory  = new RedisFactory(RedisClient.create(), mock[LocationService], wiring)
  private val subscriber: EventSubscriber = redisFactory.subscriber(host, port)

  lazy val reporterExecutor: ExecutorService = Executors.newFixedThreadPool(1)
  def reporter(name: String): TestRateReporter = {
    val r = new TestRateReporter(name)
    reporterExecutor.execute(r)
    r
  }
  val rep = reporter("consumer")

  val histogram: Histogram = new Histogram(SECONDS.toNanos(10), 3)

  def startSubscription(id: Int): Future[Done] = {
    val endEventName = EventName(s"${EventUtils.endEventS}-$id")
    val eventKeys    = Set(EventKey(s"$testEventKey-$id"), EventKey(s"${prefix.prefix}.$endEventS-$id"))
    val eventsToDrop = warmupMsgs + eventKeys.size //inclusive of latest events from subscription

    subscriber
      .subscribe(eventKeys)
      .drop(eventsToDrop)
      .takeWhile {
        case SystemEvent(_, _, `endEventName`, _, _) ⇒ false
        case _                                       ⇒ true
      }
      .statefulMapConcat { () ⇒
        var startTime       = 0L
        var totalTime       = 0L
        var eventsReceived  = 0L
        var lastId          = 0
        var outOfOrderCount = 0

        event ⇒
          if (eventsReceived == 0)
            startTime = getNanos(Instant.now()).toLong

          eventsReceived += 1
          val currentTime = getNanos(Instant.now()).toLong
          totalTime = currentTime - startTime

          rep.onMessage(1, 100)

          val latency = (getNanos(Instant.now()) - getNanos(event.eventTime.time)).toLong
          try {
            histogram.recordValue(latency)
          } catch {
            case e: ArrayIndexOutOfBoundsException ⇒
          }

          val currentId = event.eventId.id.toInt
          val inOrder   = currentId >= lastId
          lastId = currentId

          if (!inOrder) {
            outOfOrderCount += 1
          }

          List(event)
      }
      .watchTermination()(Keep.right)
      .runWith(Sink.ignore)
  }

  def subscribe() = (1 to 40).map(startSubscription)

  Await.result(Future.sequence(subscribe()), 20.minute)
}
