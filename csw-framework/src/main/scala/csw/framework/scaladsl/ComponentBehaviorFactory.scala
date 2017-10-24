package csw.framework.scaladsl

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.framework.internal.component.ComponentBehaviorImmutable
import csw.messages.IdleMessage.Initialize
import csw.messages.PubSub.PublisherMessage
import csw.messages.RunningMessage.DomainMessage
import csw.messages.framework.ComponentInfo
import csw.messages.params.states.CurrentState
import csw.messages.{ComponentMessage, FromComponentLifecycleMessage}
import csw.services.location.scaladsl.LocationService

import scala.reflect.ClassTag

abstract class ComponentBehaviorFactory[Msg <: DomainMessage: ClassTag] {

  protected[framework] def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      pubSubRef: ActorRef[PublisherMessage[CurrentState]],
      locationService: LocationService
  ): ComponentHandlers[Msg]

  def make(
      compInfo: ComponentInfo,
      supervisor: ActorRef[FromComponentLifecycleMessage],
      pubSubRef: ActorRef[PublisherMessage[CurrentState]],
      locationService: LocationService
  ): Behavior[Nothing] =
    Actor
      .deferred[ComponentMessage] { ctx ⇒
        val beh = ComponentBehaviorImmutable
          .componentBehaviorImmutable(
            ctx,
            compInfo,
            supervisor,
            handlers(ctx, compInfo, pubSubRef, locationService),
            locationService
          )
        val tla = ctx.spawn(beh, compInfo.name)
        tla ! Initialize
        Actor.same
      }
      .narrow
}
