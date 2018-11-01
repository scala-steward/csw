package romaine

import io.lettuce.core.{RedisClient, RedisURI}
import romaine.async.RedisAsyncApi
import romaine.codec.{RomaineByteCodec, RomaineRedisCodec}
import romaine.exceptions.RedisServerNotAvailable
import romaine.reactive.RedisSubscriptionApi

import scala.async.Async
import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class RomaineFactory(redisClient: RedisClient)(implicit val ec: ExecutionContext) {
  def redisAsyncApi[K: RomaineByteCodec, V: RomaineByteCodec](redisURI: RedisURI): RedisAsyncApi[K, V] = new RedisAsyncApi(
    {
      try {
        redisClient.connect(new RomaineRedisCodec[K, V], redisURI).async()
      } catch {
        case NonFatal(ex) ⇒ throw RedisServerNotAvailable(ex.getCause)
      }
    }
  )

  def redisSubscriptionApi[K: RomaineByteCodec, V: RomaineByteCodec](redisURI: RedisURI): RedisSubscriptionApi[K, V] =
    new RedisSubscriptionApi(
      () =>
        Async.async {
          val connectionF =
            redisClient
              .connectPubSubAsync(new RomaineRedisCodec[K, V], redisURI)
              .toScala
              .recover {
                case NonFatal(ex) ⇒ throw RedisServerNotAvailable(ex.getCause)
              }

          await(connectionF).reactive()
      }
    )

}
