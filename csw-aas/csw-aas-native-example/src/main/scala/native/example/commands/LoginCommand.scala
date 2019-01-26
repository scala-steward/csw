package native.example.commands
import native.example.AuthWiring
import org.backuity.clist._

object LoginCommand extends Command(name = "login", description = "performs user authentication") with AppCommand {
  var console: Boolean = opt[Boolean](default = false, description = "instead of using browser, prompts credentials on console")

  override def run(): Unit = {
    if (console) {
      if (AuthWiring.nativeAuthAdapter.loginCommandLine())
        println("SUCCESS : Logged in successfully")
    } else {
      AuthWiring.nativeAuthAdapter.login()
    }
  }
}
