package romaine.reactive

import akka.stream.scaladsl.Source
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.async.RedisAsyncApi
import romaine.codec.RomaineStringCodec
import romaine.extensions.SourceExtensions.RichSource
import romaine.reactive.RedisOperation._

sealed trait RedisOperation extends Product with Serializable

object RedisOperation {
  case object Set     extends RedisOperation
  case object Expired extends RedisOperation
  case object Unknown extends RedisOperation
}

import scala.concurrent.{ExecutionContext, Future}

trait KeySpaceFactory[T] {
  def fromString(string: String): T
}
object KeySpaceFactory {
  def apply[T](implicit x: KeySpaceFactory[T]): KeySpaceFactory[T] = x
}

trait KeyspaceKey {
  def value: String
}

case class Keyspace0Key(value: String) extends KeyspaceKey {
  override def toString: String = s"${Keyspace0Key.Prefix}$value"
}
object Keyspace0Key {
  implicit val keySpaceFactory: KeySpaceFactory[Keyspace0Key] = Keyspace0Key.apply
  val Prefix                                                  = "__keyspace@0__:"
  implicit val codec: RomaineStringCodec[Keyspace0Key] =
    RomaineStringCodec.codec(_.toString, x ⇒ Keyspace0Key(x.stripPrefix(Prefix)))
}

case class Keyspace1Key(value: String) extends KeyspaceKey {
  override def toString: String = s"${Keyspace0Key.Prefix}$value"
}
object Keyspace1Key {
  implicit val keySpaceFactory: KeySpaceFactory[Keyspace1Key] = Keyspace1Key.apply
  val Prefix                                                  = "__keyspace@1__:"
  implicit val codec: RomaineStringCodec[Keyspace1Key] =
    RomaineStringCodec.codec(_.toString, x ⇒ Keyspace1Key(x.stripPrefix(Prefix)))
}

class RedisKeySpaceApi[K: RomaineStringCodec, V: RomaineStringCodec, K1 <: KeyspaceKey: KeySpaceFactory](
    redisSubscriptionApi: RedisSubscriptionApi[K1, RedisOperation],
    redisAsyncApi: RedisAsyncApi[K, V]
)(implicit ec: ExecutionContext) {
  def watchKeyspaceValue(
      keys: List[K],
      overflowStrategy: OverflowStrategy
  ): Source[RedisResult[K, Option[V]], RedisSubscription] =
    redisSubscriptionApi
      .psubscribe(keys.map(x ⇒ KeySpaceFactory[K1].fromString(RomaineStringCodec[K].toString(x))), overflowStrategy)
      .filterNot(_.value == Unknown)
      .mapAsync(1) { pm =>
        val k = RomaineStringCodec[K].fromString(pm.key.value)
        pm.value match {
          case Set     => redisAsyncApi.get(k).map(valueOpt ⇒ (k, valueOpt))
          case Expired => Future((k, None))
        }
      }
      .collect {
        case (k, v) ⇒ RedisResult(k, v)
      }
      .distinctUntilChanged

}
//todo: support for delete and expired, etc
//todo: RedisWatchSubscription try to remove type parameter
