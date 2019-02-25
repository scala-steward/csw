package csw.event.client.internal.redis

import akka.stream.Materializer
import csw.event.api.scaladsl.EventService
import csw.event.client.internal.commons.serviceresolver.EventServiceResolver
import csw.params.events.{Event, EventKey}
import io.lettuce.core.{RedisClient, RedisURI}
import romaine.RomaineFactory
import romaine.async.RedisAsyncApi

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * Implementation of [[csw.event.api.scaladsl.EventService]] which provides handle to [[csw.event.api.scaladsl.EventPublisher]]
 * and [[csw.event.api.scaladsl.EventSubscriber]] backed by Redis
 *
 * @param eventServiceResolver to get the connection information of event service
 * @param masterId the Id used by Redis Sentinel to identify the master
 * @param redisClient the client instance of [[io.lettuce.core.RedisClient]]
 * @param executionContext the execution context to be used for performing asynchronous operations
 * @param mat the materializer to be used for materializing underlying streams
 */
class RedisEventService(eventServiceResolver: EventServiceResolver, masterId: String, redisClient: RedisClient)(
    implicit val executionContext: ExecutionContext,
    mat: Materializer
) extends EventService {

  private val awaitDuration = 10.seconds

  import EventRomaineCodecs._
  private val romaineFactory = new RomaineFactory(redisClient)

  override def makeNewPublisher(): RedisPublisher = {
    import EventRomaineCodecs._
    val uri                                    = redisURI()
    val romaineFactory                         = new RomaineFactory(redisClient)
    val asyncApi: RedisAsyncApi[String, Event] = Await.result(romaineFactory.redisAsyncApi(uri), awaitDuration)

    new RedisPublisher(uri, redisClient, asyncApi)
  }

  override def makeNewSubscriber(): RedisSubscriber = {
    val uri                                      = redisURI()
    val asyncApi: RedisAsyncApi[EventKey, Event] = Await.result(romaineFactory.redisAsyncApi(uri), awaitDuration)
    new RedisSubscriber(uri, redisClient, asyncApi, romaineFactory)
  }

  // resolve event service every time before creating a new publisher or subscriber
  private def redisURI(): RedisURI = {
    val uri = Await.result(eventServiceResolver.uri(), awaitDuration)
    RedisURI.Builder.sentinel(uri.getHost, uri.getPort, masterId).build()
  }
}
