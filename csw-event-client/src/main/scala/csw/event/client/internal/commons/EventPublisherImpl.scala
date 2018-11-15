package csw.event.client.internal.commons

import akka.Done
import akka.actor.Cancellable
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import csw.event.api.exceptions.PublishFailure
import csw.event.api.scaladsl.EventPublisher
import csw.params.events.Event

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

/**
 * An implementation of [[csw.event.api.scaladsl.EventPublisher]] API which uses Redis as the provider for publishing
 * and subscribing events.
 */
class EventPublisherImpl(publishApi: PublishApi)(implicit mat: Materializer, ec: ExecutionContext) extends EventPublisher {
  // inorder to preserve the order of publishing events, the parallelism level is maintained to 1
  private val parallelism = 1

  private val eventPublisherUtil = new EventPublisherUtil(publishApi)

  override def publish(event: Event): Future[Done] = eventPublisherUtil.publish(event)

  override def publish[Mat](source: Source[Event, Mat]): Mat =
    eventPublisherUtil.publishFromSource(source, parallelism, None)

  override def publish[Mat](source: Source[Event, Mat], onError: PublishFailure ⇒ Unit): Mat =
    eventPublisherUtil.publishFromSource(source, parallelism, Some(onError))

  override def publish(eventGenerator: ⇒ Event, every: FiniteDuration): Cancellable =
    publish(eventPublisherUtil.eventSource(eventGenerator, every))

  override def publish(eventGenerator: ⇒ Event, every: FiniteDuration, onError: PublishFailure ⇒ Unit): Cancellable =
    publish(eventPublisherUtil.eventSource(eventGenerator, every), onError)

  override def publishAsync(eventGenerator: ⇒ Future[Event], every: FiniteDuration): Cancellable =
    publish(eventPublisherUtil.eventSourceAsync(eventGenerator, every))

  override def publishAsync(eventGenerator: ⇒ Future[Event], every: FiniteDuration, onError: PublishFailure ⇒ Unit): Cancellable =
    publish(eventPublisherUtil.eventSourceAsync(eventGenerator, every), onError)

  override def shutdown(): Future[Done] = eventPublisherUtil.shutdown()
}
