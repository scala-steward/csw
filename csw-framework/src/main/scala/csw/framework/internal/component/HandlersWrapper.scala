package csw.framework.internal.component
import csw.command.client.messages.CommandMessage
import csw.framework.scaladsl.ComponentHandlers
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandResponse.{SubmitResponse, ValidateCommandResponse}

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Wrapper for lifecycleHandlers which logs and calls appropriate handler method
 */
class HandlersWrapper(componentHandlers: ComponentHandlers, val log: Logger) {

  def initialize(lifecycleState: ComponentLifecycleState)(implicit ec: ExecutionContext): Future[Unit] = async {
    log.info("Invoking lifecycle handler's initialize hook")
    await(componentHandlers.initialize())
    log.debug(
      s"Component TLA is changing lifecycle state from [$lifecycleState] to [${ComponentLifecycleState.Initialized}]"
    )
  }

  def validateCommand(commandMessage: CommandMessage): ValidateCommandResponse = {
    log.info(s"Invoking lifecycle handler's validateCommand hook with msg :[$commandMessage]")
    componentHandlers.validateCommand(commandMessage.command)
  }

  def submit(commandMessage: CommandMessage): SubmitResponse = {
    log.info(s"Invoking lifecycle handler's onSubmit hook with msg :[$commandMessage]")
    componentHandlers.onSubmit(commandMessage.command)
  }

  def oneway(commandMessage: CommandMessage): Unit = {
    log.info(s"Invoking lifecycle handler's onOneway hook with msg :[$commandMessage]")
    componentHandlers.onOneway(commandMessage.command)
  }

  def shutdown(): Future[Unit] = {
    log.info("Invoking lifecycle handler's onShutdown hook")
    componentHandlers.onShutdown()
  }

  def goOffline(): Unit = {
    // process only if the component is online currently
    if (componentHandlers.isOnline) {
      componentHandlers.isOnline = false
      log.info("Invoking lifecycle handler's onGoOffline hook")
      componentHandlers.onGoOffline()
      log.debug(s"Component TLA is Offline")
    }
  }

  def goOnline(): Unit = {
    // process only if the component is offline currently
    if (!componentHandlers.isOnline) {
      componentHandlers.isOnline = true
      log.info("Invoking lifecycle handler's onGoOnline hook")
      componentHandlers.onGoOnline()
      log.debug(s"Component TLA is Online")
    }
  }
}
