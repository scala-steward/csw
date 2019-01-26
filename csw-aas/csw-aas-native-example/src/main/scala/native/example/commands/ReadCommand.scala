package native.example.commands
import org.backuity.clist._

object ReadCommand extends Command(name = "read", description = "reads data from server") with AppCommand {
  var fileId: Long = arg[Long]()

  override def run(): Unit = {
    println(s"read file id $fileId")
  }
}
