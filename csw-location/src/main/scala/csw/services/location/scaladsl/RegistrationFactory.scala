package csw.services.location.scaladsl

import akka.typed.ActorRef
import csw.messages.location.Connection.AkkaConnection
import csw.services.location.models.AkkaRegistration
import csw.services.logging.internal.LogControlMessages

class RegistrationFactory(logAdminActorRef: ActorRef[LogControlMessages]) {
  def akkaTyped(
      akkaConnection: AkkaConnection,
      actorRef: ActorRef[Nothing]
  ): AkkaRegistration =
    AkkaRegistration.apply(akkaConnection, actorRef, logAdminActorRef)
}
