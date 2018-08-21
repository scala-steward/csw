package romaine

import io.lettuce.core.{RedisClient, RedisURI}
import romaine.async.RedisAsyncApi
import romaine.codec.{RomaineByteCodec, RomaineRedisCodec}
import romaine.reactive.RedisSubscriptionApi

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RomaineFactory(redisClient: RedisClient)(implicit val ec: ExecutionContext) {
  def redisAsyncApi[K: RomaineByteCodec, V: RomaineByteCodec](redisURI: RedisURI): Future[RedisAsyncApi[K, V]] = {
    redisClient
      .connectAsync(new RomaineRedisCodec[K, V], redisURI)
      .toScala
      .map(connection => new RedisAsyncApi(connection.async()))
  }

  def redisSubscriptionApi[K: RomaineByteCodec, V: RomaineByteCodec](redisURI: RedisURI): Future[RedisSubscriptionApi[K, V]] = {
    redisClient
      .connectPubSubAsync(new RomaineRedisCodec[K, V], redisURI)
      .toScala
      .map(connection => new RedisSubscriptionApi(() => connection.reactive()))
  }
}