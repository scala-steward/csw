package csw.services.location.scaladsl

import akka.typed.ActorRef
import csw.services.location.models.AkkaRegistration
import csw.services.location.models.Connection.AkkaConnection

import scala.reflect.ClassTag

class RegistrationFactory {
  def akkaTyped[M: ClassTag](akkaConnection: AkkaConnection, actorRef: ActorRef[M]): AkkaRegistration[M] =
    AkkaRegistration(akkaConnection, actorRef)

  def jAkkaTyped[M](akkaConnection: AkkaConnection, actorRef: ActorRef[M], klass: Class[M]): AkkaRegistration[M] =
    AkkaRegistration(akkaConnection, actorRef)(ClassTag(klass))

  def jAkkaTyped(akkaConnection: AkkaConnection, actorRef: ActorRef[AnyRef]): AkkaRegistration[AnyRef] =
    AkkaRegistration(akkaConnection, actorRef)
}
