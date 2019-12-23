package csw.logging.client.scaladsl

import akka.actor
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.adapter.TypedActorRefOps
import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import csw.logging.api.scaladsl.Logger
import csw.logging.client.internal.LoggerImpl
import csw.logging.client.javadsl.JLoggerFactory
import csw.prefix.models.Prefix

abstract class BaseLoggerFactory private[logging] (maybePrefix: Option[Prefix]) {
  def getLogger[T](ctx: ActorContext[T]): Logger = new LoggerImpl(maybePrefix, Some(actorPath(ctx.self.toClassic)))
  def getLogger(ctx: actor.ActorContext): Logger = new LoggerImpl(maybePrefix, Some(actorPath(ctx.self)))
  def getLogger: Logger                          = new LoggerImpl(maybePrefix, None)

  private def actorPath(actorRef: ActorRef): String = ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString
}

/**
 * When using the `LoggerFactory`, log statements will have `@componentName` tag with provided `prefix`
 *
 * @param prefix to appear in log statements
 */
class LoggerFactory(prefix: Prefix) extends BaseLoggerFactory(Some(prefix)) {

  /**
   * Returns the java API for this instance of LoggerFactory
   */
  def asJava: JLoggerFactory = new JLoggerFactory(prefix)
}

/**
 * When using the `GenericLoggerFactory`, log statements will not have `@componentName` tag
 */
object GenericLoggerFactory extends BaseLoggerFactory(None)
