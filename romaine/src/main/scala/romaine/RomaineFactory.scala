package romaine

import io.lettuce.core.{RedisClient, RedisURI}
import romaine.async.RedisAsyncApi
import romaine.codec.{RomaineByteCodec, RomaineRedisCodec}
import romaine.exceptions.RedisServerNotAvailable
import romaine.reactive.RedisSubscriptionApi

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RomaineFactory(redisClient: RedisClient)(implicit val ec: ExecutionContext) {
  def redisAsyncApi[K: RomaineByteCodec, V: RomaineByteCodec](redisURI: RedisURI): Future[RedisAsyncApi[K, V]] =
    initAsyncCommand[K, V](redisURI).map(new RedisAsyncApi(_))

  def redisSubscriptionApi[K: RomaineByteCodec, V: RomaineByteCodec](redisURI: RedisURI): RedisSubscriptionApi[K, V] =
    new RedisSubscriptionApi(initReactiveCommand(redisURI))

  private def initReactiveCommand[K: RomaineByteCodec, V: RomaineByteCodec](redisURI: RedisURI) = { () =>
    init { () =>
      redisClient.connectPubSubAsync(new RomaineRedisCodec[K, V], redisURI).toScala
    }.map(_.reactive())
  }

  private def initAsyncCommand[K: RomaineByteCodec, V: RomaineByteCodec](redisURI: RedisURI) =
    init { () =>
      redisClient.connectAsync(new RomaineRedisCodec[K, V], redisURI).toScala
    }.map(_.async())

  private def init[T](conn: () ⇒ Future[T]): Future[T] = Future.unit.flatMap(_ => conn()).recover {
    case NonFatal(ex) ⇒ throw RedisServerNotAvailable(ex.getCause)
  }
}
