package csw.services.location.models

import java.net.URI

import acyclic.skipped
import akka.typed.ActorRef
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}

import scala.reflect.{classTag, ClassTag}

/**
 * Location represents a live Connection along with its URI
 */
sealed abstract class Location extends TmtSerializable {
  def connection: Connection
  def uri: URI
}

/**
 * Represents a live Akka connection of an Actor
 */
final case class AkkaLocation[T: ClassTag](connection: AkkaConnection, uri: URI, actorRef: ActorRef[T])
    extends Location {
  val tag: ClassTag[T] = classTag[T]
  println(tag)
  def typedRef[M: ClassTag]: ActorRef[M] =
    if (tag.runtimeClass.isAssignableFrom(classTag[M].runtimeClass)) {
      actorRef.asInstanceOf[ActorRef[M]]
    } else {
      throw new OutOfMemoryError(s"ActorRef of $tag can not be cast to ActorRef of ${classTag[M]}")
    }

  def jTypeRef[M <: AnyRef](klass: Class[M]): ActorRef[M] = typedRef(ClassTag(klass))
  def jTypeRef: ActorRef[Nothing]                         = typedRef[Nothing]
}

/**
 * Represents a live Tcp connection
 */
final case class TcpLocation(connection: TcpConnection, uri: URI) extends Location

/**
 * Represents a live Http connection
 */
final case class HttpLocation(connection: HttpConnection, uri: URI) extends Location
