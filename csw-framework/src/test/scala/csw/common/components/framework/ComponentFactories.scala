package csw.common.components.framework

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers, CurrentStatePublisher}
import csw.common.framework.ComponentInfo
import csw.common.scaladsl.TopLevelActorMessage
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

class SampleComponentBehaviorFactory extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ActorContext[TopLevelActorMessage],
                                  componentInfo: ComponentInfo,
                                  commandResponseManager: CommandResponseManager,
                                  currentStatePublisher: CurrentStatePublisher,
                                  locationService: LocationService,
                                  loggerFactory: LoggerFactory): ComponentHandlers =
    new SampleComponentHandlers(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, loggerFactory)
}

class ComponentBehaviorFactoryToSimulateFailure extends ComponentBehaviorFactory {
  protected override def handlers(ctx: ActorContext[TopLevelActorMessage],
                                  componentInfo: ComponentInfo,
                                  commandResponseManager: CommandResponseManager,
                                  currentStatePublisher: CurrentStatePublisher,
                                  locationService: LocationService,
                                  loggerFactory: LoggerFactory): ComponentHandlers =
    new ComponentHandlerToSimulateFailure(
      ctx,
      componentInfo,
      commandResponseManager,
      currentStatePublisher,
      locationService,
      loggerFactory
    )
}
