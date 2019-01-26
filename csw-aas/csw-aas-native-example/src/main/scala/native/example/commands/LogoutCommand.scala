package native.example.commands
import native.example.AuthWiring
import org.backuity.clist._

object LogoutCommand extends Command(name = "logout", description = "logs user out") with AppCommand {
  override def run(): Unit = {
    AuthWiring.nativeAuthAdapter.logout()
    println("SUCCESS : Logged in successfully")
  }
}
