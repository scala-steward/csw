package csw.services.event.internal.redis

import akka.Done
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitches, Materializer}
import csw.messages.ccs.events._
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisSubscriber(redisGateway: RedisGateway)(implicit ec: ExecutionContext, protected val mat: Materializer)
    extends EventSubscriber {
  private lazy val asyncConnectionF = redisGateway.asyncConnectionF()

  override def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription] = {
    val connectionF = redisGateway.reactiveConnectionF()

    val sourceF = async {
      val connection = await(connectionF)
      connection.subscribe(eventKeys.toSeq: _*).subscribe()
      Source.fromPublisher(connection.observeChannels(OverflowStrategy.LATEST)).map(_.getMessage)
    }

    val latestEventStream = Source.fromFuture(get(eventKeys)).mapConcat(identity)
    val eventStream       = Source.fromFutureSource(sourceF)

    latestEventStream
      .concatMat(eventStream)(Keep.right)
      .viaMat(KillSwitches.single)(Keep.both)
      .watchTermination()(Keep.both)
      .mapMaterializedValue {
        case ((eventStreamReady, killSwitch), terminationSignal) ⇒
          new EventSubscription {
            override def isReady: Future[Done] = eventStreamReady.map(_ ⇒ Done)

            override def unsubscribe(): Future[Done] = async {
              val commands = await(connectionF)
              await(commands.unsubscribe(eventKeys.toSeq: _*).toFuture.toScala)
              killSwitch.shutdown()
              await(terminationSignal)
            }
          }
      }
  }

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = {
    Future.sequence(eventKeys.map(get))
  }

  override def get(eventKey: EventKey): Future[Event] = async {
    val connection = await(asyncConnectionF)
    val event      = await(connection.get(eventKey).toScala)

    if (event == null) Event.invalidEvent else event
  }
}
