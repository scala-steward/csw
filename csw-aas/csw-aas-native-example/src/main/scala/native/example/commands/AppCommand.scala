package native.example.commands

import org.backuity.clist._

trait AppCommand { this: Command =>
  def run(): Unit
}
