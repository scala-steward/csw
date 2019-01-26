package native.example
import native.example.commands._
import org.backuity.clist._

object Main extends App {
  val command: Option[AppCommand] = Cli
    .parse(args)
    .withProgramName("demo-cli")
    .withCommands(LoginCommand, LogoutCommand, ReadCommand, WriteCommand)

  command.foreach(_.run())

  AuthWiring.actorSystem.terminate()
}
