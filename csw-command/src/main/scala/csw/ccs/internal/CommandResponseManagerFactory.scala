package csw.ccs.internal

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.messages.framework.ComponentInfo
import csw.messages.{CommandResponseManagerMessage, SupervisorMessage}

object CommandResponseManagerFactory {
  def make(
      ctx: ActorContext[SupervisorMessage],
      actorName: String,
      componentInfo: ComponentInfo
  ): ActorRef[CommandResponseManagerMessage] = {
    ctx
      .spawn(
        Actor.mutable[CommandResponseManagerMessage](ctx ⇒ new CommandResponseManagerBehavior(ctx, componentInfo)),
        actorName
      )
  }
}
