package romaine.async

import akka.Done
import io.lettuce.core.api.async.RedisAsyncCommands
import romaine.RedisResult
import romaine.extensions.FutureExtensions.RichFuture

import scala.collection.JavaConverters.{iterableAsScalaIterableConverter, mapAsJavaMapConverter}
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.compat.java8.OptionConverters.RichOptionalGeneric
import scala.concurrent.{ExecutionContext, Future}

class RedisAsyncApi[K, V](redisAsyncCommands: RedisAsyncCommands[K, V])(implicit ec: ExecutionContext) {

  def set(key: K, value: V): Future[Done] =
    redisAsyncCommands.set(key, value).toScala.failWith(s"Redis 'SET' operation failed for [key:$key value:$value]")

  def mset(map: Map[K, V]): Future[Done] =
    redisAsyncCommands.mset(map.asJava).toScala.failWith(s"Redis 'MSET' operation failed for [map: $map]")

  def setex(key: K, seconds: Long, value: V): Future[Done] =
    redisAsyncCommands
      .setex(key, seconds, value)
      .toScala
      .failWith(s"Redis 'SETEX' operation failed for [key: $key, value: $value]")

  def get(key: K): Future[Option[V]] = redisAsyncCommands.get(key).toScala.map(Option(_))

  def mget(keys: List[K]): Future[List[RedisResult[K, Option[V]]]] =
    redisAsyncCommands
      .mget(keys: _*)
      .toScala
      .map(_.asScala.map(kv ⇒ RedisResult(kv.getKey, kv.optional().asScala)).toList)

  def keys(key: K): Future[List[K]] = redisAsyncCommands.keys(key).toScala.map(_.asScala.toList)

  def exists(keys: K*): Future[Boolean] = redisAsyncCommands.exists(keys: _*).toScala.map(_ == keys.size)

  def del(keys: List[K]): Future[Long] = redisAsyncCommands.del(keys: _*).toScala.map(_.toLong)

  def pdel(pattern: K): Future[Long] =
    keys(pattern).flatMap(matchedKeys ⇒ if (matchedKeys.nonEmpty) del(matchedKeys) else Future.successful(0))

  def publish(key: K, value: V): Future[Long] = redisAsyncCommands.publish(key, value).toScala.map(_.toLong)

  def quit(): Future[String] = redisAsyncCommands.quit().toScala
}
