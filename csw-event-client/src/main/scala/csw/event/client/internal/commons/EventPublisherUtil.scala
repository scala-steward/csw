package csw.event.client.internal.commons

import akka.Done
import akka.actor.{Cancellable, PoisonPill}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import csw.event.api.exceptions.PublishFailure
import csw.params.events.Event

import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

/**
 * Utility class to provided common functionalities to different implementations of EventPublisher
 */
class EventPublisherUtil(publishApi: PublishApi)(implicit ec: ExecutionContext, mat: Materializer) {

  private val logger = EventServiceLogger.getLogger

  private val (actorRef, stream) = Source.actorRef[(Event, Promise[Done])](1024, OverflowStrategy.dropHead).preMaterialize()

  private val streamTermination: Future[Done] =
    stream
      .mapAsync(1) {
        case (e, p) =>
          publishApi.publish(e).map(p.trySuccess).recover {
            case ex => p.tryFailure(ex)
          }
      }
      .runForeach(_ => ())

  def publishSingle(event: Event): Future[Done] = {
    if (streamTermination.isCompleted) {
      Future.failed(PublishFailure(event, new RuntimeException("Publisher is shutdown")))
    } else {
      val p = Promise[Done]
      actorRef ! ((event, p))
      p.future
    }
  }

  def publishSource[Mat](source: Source[Event, Mat], parallelism: Int, maybeOnError: Option[PublishFailure ⇒ Unit]): Mat = {
    source
      .mapAsync(parallelism) { event ⇒
        publishWithRecovery(event, maybeOnError)
      }
      .to(Sink.ignore)
      .run()
  }

  // create an akka stream source out of eventGenerator function
  def eventSource(eventGenerator: => Event, every: FiniteDuration): Source[Event, Cancellable] =
    eventSourceAsync(Future.successful(eventGenerator), every)

  // create an akka stream source out of eventGenerator function
  def eventSourceAsync(eventGenerator: => Future[Event], every: FiniteDuration): Source[Event, Cancellable] =
    Source.tick(0.millis, every, ()).mapAsync(1)(_ => withErrorLogging(eventGenerator))

  private def publishWithRecovery(event: Event, maybeOnError: Option[PublishFailure ⇒ Unit]) =
    publishApi.publish(event).recover[Done] {
      case failure @ PublishFailure(_, _) ⇒
        maybeOnError.foreach(onError ⇒ onError(failure))
        Done
    }

  def shutdown(): Future[Done] = {
    actorRef ! PoisonPill
    publishApi.shutdown()
  }

  // log error for any exception from provided eventGenerator
  private def withErrorLogging(eventGenerator: => Future[Event]): Future[Event] =
    eventGenerator.recover {
      case NonFatal(ex) ⇒
        logger.error(ex.getMessage, ex = ex)
        throw ex
    }

}
