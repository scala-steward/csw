package csw.alarm.client.internal.configparser
import com.typesafe.config.ConfigFactory
import csw.alarm.api.exceptions.ConfigParseException
import csw.alarm.client.internal.helpers.AlarmTestData
import csw.commons.tagobjects.FileSystemSensitive
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-451: Create set of alarms based on Configuration file
// DEOPSCSW-452: Represent alarm in a model as defined in Configuration file
class ConfigParserTest extends FunSuite with Matchers with AlarmTestData {

  test("should able to parse valid alarm metadata's config file") {
    val config = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")

    val expectedAlarmMetadataSet = Set(tromboneAxisHighLimitAlarm, tromboneAxisLowLimitAlarm, cpuExceededAlarm, cpuIdleAlarm)

    val actualAlarmMetadataSet = ConfigParser.parseAlarmMetadataSet(config)

    actualAlarmMetadataSet.alarms shouldEqual expectedAlarmMetadataSet
  }

  test("should throw Exception while parsing invalid alarms metadata config file", FileSystemSensitive) {
    val config = ConfigFactory.parseResources("test-alarms/invalid-alarms.conf")

    val parseException = the[ConfigParseException] thrownBy ConfigParser.parseAlarmMetadataSet(config)

    parseException.reasons.map(_.split("invalid-alarms.conf, ").tail.head).toSet shouldEqual Set(
      """at path: /alarms/2/alarmType: error: instance value ("Invalid") not found in enum (possible values: ["Absolute","BitPattern","Calculated","Deviation","Discrepancy","Instrument","RateChange","RecipeDriven","Safety","Statistical","System"]) (schema: config:/alarm-schema.conf#:/properties/alarmType)"""
    )
  }

}
