package csw.alarm.cli.args
import java.io.ByteArrayOutputStream
import java.nio.file.Paths

import csw.alarm.api.models.AlarmSeverity.Major
import csw.alarm.cli.BuildInfo
import csw.params.core.models.Subsystem.NFIRAOS
import org.scalatest.{FunSuite, Matchers}

class ArgsParserTest extends FunSuite with Matchers {

  //Capture output/error generated by the parser, for cleaner test output. If interested, errCapture.toString will return capture errors.
  private val outCapture = new ByteArrayOutputStream
  private val errCapture = new ByteArrayOutputStream

  def silentParse(options: Array[String]): Option[Options] = Console.withOut(outCapture) {
    Console.withErr(errCapture) {
      new ArgsParser(BuildInfo.name).parse(options.toList)
    }
  }

  test("parse without specifying operation") {
    val options = Array("")
    silentParse(options) shouldBe None
  }

  test("parse init command without any options") {
    val options = Array("init")
    silentParse(options) shouldBe None
  }

  test("parse init command with only mandatory options") {
    val options = Array("init", "/a/b/c")
    silentParse(options) should contain(
      Options(cmd = "init", filePath = Some(Paths.get("/a/b/c")))
    )
  }

  test("parse init command with all options") {
    val options = Array("init", "/a/b/c", "--local", "--reset", "--locationHost", "location.server")
    silentParse(options) should contain(
      Options(
        cmd = "init",
        filePath = Some(Paths.get("/a/b/c")),
        isLocal = true,
        reset = true,
        locationHost = "location.server"
      )
    )
  }

  test(s"parse severity without sub command") {
    val options = Array("severity")
    silentParse(options) shouldBe None
  }

  test("parse set severity command") {
    val options = Array(
      "severity",
      "set",
      "Major",
      "--subsystem",
      "NFIRAOS",
      "--component",
      "trombone",
      "--name",
      "tromboneAxisHighLimitAlarm"
    )

    silentParse(options) should contain(
      Options(
        cmd = "severity",
        subCmd = "set",
        maybeSubsystem = Some(NFIRAOS),
        maybeComponent = Some("trombone"),
        maybeAlarmName = Some("tromboneAxisHighLimitAlarm"),
        severity = Some(Major)
      )
    )
  }

  test("parse get severity command") {
    val options = Array(
      "severity",
      "get",
      "--subsystem",
      "NFIRAOS",
      "--component",
      "trombone",
      "--name",
      "tromboneAxisHighLimitAlarm"
    )

    silentParse(options) should contain(
      Options(
        cmd = "severity",
        subCmd = "get",
        maybeSubsystem = Some(NFIRAOS),
        maybeComponent = Some("trombone"),
        maybeAlarmName = Some("tromboneAxisHighLimitAlarm")
      )
    )
  }

  test("parse subscribe severity command") {
    val options = Array(
      "severity",
      "subscribe",
      "--subsystem",
      "NFIRAOS",
      "--component",
      "trombone",
      "--name",
      "tromboneAxisHighLimitAlarm"
    )

    silentParse(options) should contain(
      Options(
        cmd = "severity",
        subCmd = "subscribe",
        maybeSubsystem = Some(NFIRAOS),
        maybeComponent = Some("trombone"),
        maybeAlarmName = Some("tromboneAxisHighLimitAlarm")
      )
    )
  }

  test("parse refresh severity command") {
    val options = Array(
      "severity",
      "set",
      "Major",
      "--subsystem",
      "NFIRAOS",
      "--component",
      "trombone",
      "--name",
      "tromboneAxisHighLimitAlarm",
      "--refresh"
    )

    silentParse(options) should contain(
      Options(
        cmd = "severity",
        subCmd = "set",
        maybeSubsystem = Some(NFIRAOS),
        maybeComponent = Some("trombone"),
        maybeAlarmName = Some("tromboneAxisHighLimitAlarm"),
        severity = Some(Major),
        autoRefresh = true
      )
    )
  }

  val commandsRequiringAlarmKey =
    List("acknowledge", "unacknowledge", "activate", "deactivate", "shelve", "unshelve", "reset")

  commandsRequiringAlarmKey.foreach { command =>
    test(s"parse $command command") {
      val options = Array(
        command,
        "--subsystem",
        "NFIRAOS",
        "--component",
        "trombone",
        "--name",
        "tromboneAxisHighLimitAlarm"
      )

      silentParse(options) should contain(
        Options(
          cmd = command,
          maybeSubsystem = Some(NFIRAOS),
          maybeComponent = Some("trombone"),
          maybeAlarmName = Some("tromboneAxisHighLimitAlarm")
        )
      )
    }

    test(s"parse $command without options") {
      val options = Array(command)
      silentParse(options) shouldBe None
    }
  }

  test("parse list command without options") {
    val options = Array("list")
    silentParse(options) should contain(Options(cmd = "list"))
  }

  test("parse list command") {
    val options = Array(
      "list",
      "--subsystem",
      "NFIRAOS",
      "--component",
      "trombone",
      "--name",
      "tromboneAxisHighLimitAlarm"
    )

    silentParse(options) should contain(
      Options(
        cmd = "list",
        maybeSubsystem = Some(NFIRAOS),
        maybeComponent = Some("trombone"),
        maybeAlarmName = Some("tromboneAxisHighLimitAlarm")
      )
    )
  }

  test("parse list command with status with full alarm key") {
    val options = Array(
      "list",
      "--subsystem",
      "NFIRAOS",
      "--component",
      "trombone",
      "--name",
      "tromboneAxisHighLimitAlarm",
      "--status"
    )

    silentParse(options) should contain(
      Options(
        cmd = "list",
        maybeSubsystem = Some(NFIRAOS),
        maybeComponent = Some("trombone"),
        maybeAlarmName = Some("tromboneAxisHighLimitAlarm"),
        showMetadata = false
      )
    )
  }

  test("parse list command with status with subsystem key") {
    val options = Array(
      "list",
      "--subsystem",
      "NFIRAOS",
      "--status"
    )

    silentParse(options) should contain(
      Options(
        cmd = "list",
        maybeSubsystem = Some(NFIRAOS),
        showMetadata = false
      )
    )
  }

  test("parse list command with metadata with component key") {
    val options = Array(
      "list",
      "--subsystem",
      "NFIRAOS",
      "--component",
      "trombone",
      "--metadata"
    )

    silentParse(options) should contain(
      Options(
        cmd = "list",
        maybeSubsystem = Some(NFIRAOS),
        maybeComponent = Some("trombone"),
        showStatus = false
      )
    )
  }

  test("parse list command only with subsystem and/or component options") {

    val options1 = Array("list", "--subsystem", "NFIRAOS")
    val options2 = Array("list", "--subsystem", "NFIRAOS", "--component", "trombone")

    silentParse(options1) should contain(Options(cmd = "list", maybeSubsystem = Some(NFIRAOS)))

    silentParse(options2) should contain(
      Options(
        cmd = "list",
        maybeSubsystem = Some(NFIRAOS),
        maybeComponent = Some("trombone")
      )
    )
  }

  test("parse list command  with invalid options") {
    //invalid because subsystem and component of alarm are not specified
    val options1 = Array(
      "list",
      "--name",
      "tromboneAxisHighLimitAlarm"
    )

    //invalid because subsystem of the component is not specified
    val options2 = Array(
      "list",
      "--component",
      "tromboneAxisHighLimitAlarm"
    )

    silentParse(options1) shouldBe None
    silentParse(options2) shouldBe None
  }

  test(s"parse health without sub command") {
    val options = Array("health")
    silentParse(options) shouldBe None
  }

  test("parse get health command") {
    val options = Array(
      "health",
      "get",
      "--subsystem",
      "NFIRAOS",
      "--component",
      "trombone",
      "--name",
      "tromboneAxisHighLimitAlarm"
    )

    silentParse(options) should contain(
      Options(
        cmd = "health",
        subCmd = "get",
        maybeSubsystem = Some(NFIRAOS),
        maybeComponent = Some("trombone"),
        maybeAlarmName = Some("tromboneAxisHighLimitAlarm")
      )
    )
  }

  test("parse health command only with subsystem and/or component options") {

    val options1 = Array("health", "get", "--subsystem", "NFIRAOS")
    val options2 = Array("health", "get", "--subsystem", "NFIRAOS", "--component", "trombone")

    silentParse(options1) should contain(Options(cmd = "health", subCmd = "get", maybeSubsystem = Some(NFIRAOS)))

    silentParse(options2) should contain(
      Options(
        cmd = "health",
        subCmd = "get",
        maybeSubsystem = Some(NFIRAOS),
        maybeComponent = Some("trombone")
      )
    )
  }

  test("parse subscribe health command") {
    val options = Array(
      "health",
      "subscribe",
      "--subsystem",
      "NFIRAOS",
      "--component",
      "trombone",
      "--name",
      "tromboneAxisHighLimitAlarm"
    )

    silentParse(options) should contain(
      Options(
        cmd = "health",
        subCmd = "subscribe",
        maybeSubsystem = Some(NFIRAOS),
        maybeComponent = Some("trombone"),
        maybeAlarmName = Some("tromboneAxisHighLimitAlarm")
      )
    )
  }
}
