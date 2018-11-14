package csw.event.client.internal.redis

import akka.Done
import akka.stream.Materializer
import csw.event.api.exceptions.PublishFailure
import csw.event.client.internal.commons.{EventServiceLogger, PublishApi}
import csw.params.events.Event
import io.lettuce.core.{RedisClient, RedisURI}
import romaine.RomaineFactory
import romaine.async.RedisAsyncApi

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RedisPublishApi(redisURI: Future[RedisURI], redisClient: RedisClient)(implicit mat: Materializer, ec: ExecutionContext)
    extends PublishApi {

  private val logger = EventServiceLogger.getLogger

  private val romaineFactory = new RomaineFactory(redisClient)

  import EventRomaineCodecs._
  private val asyncApi: RedisAsyncApi[String, Event] = romaineFactory.redisAsyncApi(redisURI)

  override def publish(event: Event): Future[Done] = {
    async {
      await(asyncApi.publish(event.eventKey.key, event))
      set(event, asyncApi) // set will run independent of publish
      Done
    } recover {
      case NonFatal(ex) ⇒
        val failure = PublishFailure(event, ex)
        logger.error(failure.getMessage, ex = failure)
        throw failure
    }
  }

  override def shutdown(): Future[Done] = {
    asyncApi.quit().map(_ ⇒ Done)
  }

  private def set(event: Event, commands: RedisAsyncApi[String, Event]): Future[Done] =
    commands.set(event.eventKey.key, event).recover { case NonFatal(_) ⇒ Done }

}
