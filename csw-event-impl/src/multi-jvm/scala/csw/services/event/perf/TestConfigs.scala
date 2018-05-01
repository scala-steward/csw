package csw.services.event.perf

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

import scala.concurrent.duration.{DurationLong, FiniteDuration}

class TestConfigs(config: Config) {

  //################### Common Configuration ###################
  val elements: Int = config.getInt("csw.test.EventServicePerfTest.publish-frequency.elements")
  val per: FiniteDuration = {
    val d = config.getDuration("csw.test.EventServicePerfTest.publish-frequency.per")
    FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)
  }

  val publishFrequency: FiniteDuration = (per.toMillis / elements).millis

  val warmupMsgs: Int             = config.getInt("csw.test.EventServicePerfTest.warmup")
  val totalMessagesFactor: Double = config.getDouble("csw.test.EventServicePerfTest.totalMessagesFactor")

  //################### Redis Configuration ###################
  lazy val redisEnabled: Boolean = config.getBoolean("csw.test.EventServicePerfTest.redis-enabled")
  lazy val redisHost: String     = config.getString("csw.test.EventServicePerfTest.redis.host")
  lazy val redisPort: Int        = config.getInt("csw.test.EventServicePerfTest.redis.port")

  lazy val redisHost2: String = config.getString("csw.test.EventServicePerfTest.redis-2.host")
  lazy val redisPort2: Int    = config.getInt("csw.test.EventServicePerfTest.redis-2.port")

  lazy val redisHost3: String = config.getString("csw.test.EventServicePerfTest.redis-3.host")
  lazy val redisPort3: Int    = config.getInt("csw.test.EventServicePerfTest.redis-3.port")

  lazy val redisHost4: String = config.getString("csw.test.EventServicePerfTest.redis-4.host")
  lazy val redisPort4: Int    = config.getInt("csw.test.EventServicePerfTest.redis-4.port")
  //################### Kafka Configuration ###################
  lazy val kafkaHost: String = config.getString("csw.test.EventServicePerfTest.kafka.host")
  lazy val kafkaPort: Int    = config.getInt("csw.test.EventServicePerfTest.kafka.port")

}
