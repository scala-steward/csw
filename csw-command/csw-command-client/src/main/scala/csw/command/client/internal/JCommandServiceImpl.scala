package csw.command.client.internal

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import akka.stream.javadsl.Source
import akka.util.Timeout
import csw.command.api.StateMatcher
import csw.command.api.javadsl.ICommandService
import csw.command.api.scaladsl.CommandService
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import msocket.api.Subscription

import scala.compat.java8.FunctionConverters.enrichAsScalaFromConsumer
import scala.compat.java8.FutureConverters.FutureOps
import scala.jdk.CollectionConverters._

private[command] class JCommandServiceImpl(commandService: CommandService) extends ICommandService {

  override def validate(controlCommand: ControlCommand): CompletableFuture[ValidateResponse] =
    commandService.validate(controlCommand).toJava.toCompletableFuture

  override def submit(controlCommand: ControlCommand): CompletableFuture[SubmitResponse] =
    commandService.submit(controlCommand).toJava.toCompletableFuture

  override def submitAndWait(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[SubmitResponse] =
    commandService.submitAndWait(controlCommand)(timeout).toJava.toCompletableFuture

  override def submitAllAndWait(
      controlCommand: java.util.List[ControlCommand],
      timeout: Timeout
  ): CompletableFuture[java.util.List[SubmitResponse]] =
    commandService
      .submitAllAndWait(controlCommand.asScala.toList)(timeout)
      .toJava
      .toCompletableFuture
      .thenApply(_.asJava)

  override def oneway(controlCommand: ControlCommand): CompletableFuture[OnewayResponse] =
    commandService.oneway(controlCommand).toJava.toCompletableFuture

  override def onewayAndMatch(controlCommand: ControlCommand, stateMatcher: StateMatcher): CompletableFuture[MatchingResponse] =
    commandService.onewayAndMatch(controlCommand, stateMatcher).toJava.toCompletableFuture

  override def query(commandRunId: Id): CompletableFuture[SubmitResponse] =
    commandService.query(commandRunId).toJava.toCompletableFuture

  override def queryFinal(commandRunId: Id, timeout: Timeout): CompletableFuture[SubmitResponse] =
    commandService.queryFinal(commandRunId)(timeout).toJava.toCompletableFuture

  override def subscribeCurrentState(): Source[CurrentState, Subscription] =
    commandService.subscribeCurrentState().asJava

  override def subscribeCurrentState(names: java.util.Set[StateName]): Source[CurrentState, Subscription] =
    commandService.subscribeCurrentState(names.asScala.toSet).asJava

  override def subscribeCurrentState(callback: Consumer[CurrentState]): Subscription =
    commandService.subscribeCurrentState(callback.asScala)

  override def subscribeCurrentState(
      names: java.util.Set[StateName],
      callback: Consumer[CurrentState]
  ): Subscription = commandService.subscribeCurrentState(names.asScala.toSet, callback.asScala)
}
