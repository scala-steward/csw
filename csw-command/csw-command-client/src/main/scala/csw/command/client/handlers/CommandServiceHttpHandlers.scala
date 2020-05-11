package csw.command.client.handlers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import csw.command.api.codecs.CommandServiceCodecs._
import csw.command.api.messages.CommandServiceHttpMessage
import csw.command.api.messages.CommandServiceHttpMessage._
import csw.command.api.scaladsl.CommandService
import csw.command.client.auth.{CommandPolicy, CommandRoles}
import csw.params.commands.ControlCommand
import csw.prefix.models.Prefix
import msocket.impl.post.{HttpPostHandler, ServerHttpCodecs}

class CommandServiceHttpHandlers(
    commandService: CommandService,
    securityDirectives: SecurityDirectives,
    destinationPrefix: Option[Prefix] = None,
    commandRoles: CommandRoles = CommandRoles.empty
) extends HttpPostHandler[CommandServiceHttpMessage]
    with ServerHttpCodecs {

  override def handle(request: CommandServiceHttpMessage): Route =
    request match {
      case Validate(controlCommand) => sPost(controlCommand)(complete(commandService.validate(controlCommand)))
      case Submit(controlCommand)   => sPost(controlCommand)(complete(commandService.submit(controlCommand)))
      case Oneway(controlCommand)   => sPost(controlCommand)(complete(commandService.oneway(controlCommand)))
      case Query(runId)             => complete(commandService.query(runId))
    }
  private def sPost(controlCommand: ControlCommand)(route: => Route) =
    destinationPrefix match {
      case Some(prefix) => securityDirectives.sPost(CommandPolicy(commandRoles, controlCommand, prefix))(_ => route)
      case None         => route // auth is disabled in this case
    }
}
