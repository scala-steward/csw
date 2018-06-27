package csw.messages.params.formats

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.serialization.{Serialization, SerializationExtension}
import play.api.libs.json._

object ActorRefJsonSupport extends LowPrior {

  implicit def actorRefFormat2[T](implicit actorSystem: ActorSystem): Format[ActorRef[T]] =
    actorRefFormat.asInstanceOf[Format[ActorRef[T]]]
}

trait LowPrior {
  implicit def actorRefFormat(implicit actorSystem: ActorSystem): Format[ActorRef[_]] =
    new Format[ActorRef[Any]] {
      override def writes(o: ActorRef[Any]): JsValue = JsString(Serialization.serializedActorPath(o.toUntyped))
      override def reads(json: JsValue): JsResult[ActorRef[Any]] = {
        val provider = SerializationExtension(actorSystem).system.provider
        Reads.StringReads.map[ActorRef[Any]](x => provider.resolveActorRef(x)).reads(json)
      }
    }.asInstanceOf[Format[ActorRef[_]]]
}
