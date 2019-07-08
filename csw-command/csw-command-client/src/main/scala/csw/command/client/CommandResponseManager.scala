package csw.command.client

import java.util.concurrent.{CompletableFuture, TimeoutException}

import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.messages.CommandResponseManagerMessage
import csw.command.client.messages.CommandResponseManagerMessage._
import csw.params.commands.CommandResponse.{CommandNotAvailable, QueryResponse, SubmitResponse}
import csw.params.core.models.Id

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Wrapper API for interacting with Command Response Manager of a component
 *
 * @param commandResponseManagerActor underlying actor managing command responses, completion of command, caching responses for command, etc.
 * @param actorSystem actor system for managing stream resources inside
 */
class CommandResponseManager(
    private[csw] val commandResponseManagerActor: ActorRef[CommandResponseManagerMessage]
)(implicit val actorSystem: ActorSystem[_]) {

  private implicit val scheduler: Scheduler = actorSystem.scheduler
  private implicit val ec: ExecutionContext = actorSystem.executionContext

  /**
   * Add a new command or update an existing command with the provided status
   *
   * @param cmdStatus status of command as [[csw.params.commands.CommandResponse.SubmitResponse]]
   */
  def addOrUpdateCommand(cmdStatus: SubmitResponse): Unit =
    commandResponseManagerActor ! AddOrUpdateCommand(cmdStatus)

  /**
   * Add a new sub command against another command
   *
   * @param parentRunId command identifier of original command
   * @param childRunId command identifier of sub command
   */
  def addSubCommand(parentRunId: Id, childRunId: Id): Unit =
    commandResponseManagerActor ! AddSubCommand(parentRunId, childRunId)

  /**
   * Update the status of a sub-command which will infer the status of the parent command
   *
   * @param cmdStatus status of command as [[csw.params.commands.CommandResponse.SubmitResponse]]
   */
  def updateSubCommand(cmdStatus: SubmitResponse): Unit =
    commandResponseManagerActor ! UpdateSubCommand(cmdStatus)

  /**
   * Query the current status of a command
   *
   * @param runId command identifier of command
   * @param timeout timeout duration until which this operation is expected to wait for providing a value
   * @return a future of CommandResponse
   */
  def query(runId: Id)(implicit timeout: Timeout): Future[QueryResponse] = {
    val eventualResponse: Future[QueryResponse] = commandResponseManagerActor ? (Query(runId, _))
    eventualResponse recover {
      case _: TimeoutException ⇒ CommandNotAvailable(runId)
    }
  }

  /**
   * A helper method for Java to query the current status of a command
   *
   * @param runId command identifier of command
   * @param timeout timeout duration until which this operation is expected to wait for providing a value
   * @return a future of CommandResponse
   */
  def jQuery(runId: Id, timeout: Timeout): CompletableFuture[QueryResponse] =
    query(runId)(timeout).toJava.toCompletableFuture

  /**
   * Query the final status of a command
   *
   * @param runId command identifier of command
   * @param timeout timeout duration until which this operation is expected to wait for providing a value
   * @return a future of CommandResponse
   */
  def queryFinal(runId: Id)(implicit timeout: Timeout): Future[SubmitResponse] =
    commandResponseManagerActor ? (Subscribe(runId, _))

  /**
   * A helper method for java to query the final status of a command
   *
   * @param runId command identifier of command
   * @param timeout timeout duration until which this operation is expected to wait for providing a value
   * @return a CompletableFuture of CommandResponse
   */
  def jQueryFinal(runId: Id, timeout: Timeout): CompletableFuture[SubmitResponse] =
    queryFinal(runId)(timeout).toJava.toCompletableFuture
}
