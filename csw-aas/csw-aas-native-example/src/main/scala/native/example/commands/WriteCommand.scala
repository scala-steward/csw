package native.example.commands
import org.backuity.clist._

object WriteCommand extends Command(name = "write", description = "writes data to server") with AppCommand {
  var fileId: Long    = arg[Long]()
  var content: String = arg[String]()

  override def run(): Unit = {
    println(s"write file id $fileId with content $content")
  }
}
