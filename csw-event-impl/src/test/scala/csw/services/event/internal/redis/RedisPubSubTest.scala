package csw.services.event.internal.redis

import akka.actor.ActorSystem
import com.github.sebruck.EmbeddedRedis
import csw.messages.models.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.event.RedisFactory
import csw.services.event.helpers.RegistrationFactory
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.commons.{EventServiceConnection, Wiring}
import csw.services.event.internal.{EventServicePubSubTestFramework, RateAdapterStage, RateLimiterStage}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import io.lettuce.core.RedisClient
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

class RedisPubSubTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedRedis {
  private val seedPort        = 3558
  private val redisPort       = 6379
  private val clusterSettings = ClusterAwareSettings.joinLocal(seedPort)
  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
  private val tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value, redisPort)
  locationService.register(tcpRegistration).await

  private val redis = RedisServer.builder().setting(s"bind ${clusterSettings.hostname}").port(redisPort).build()
  redis.start()

  private implicit val actorSystem: ActorSystem = clusterSettings.system
  private val redisClient                       = RedisClient.create()
  private val wiring                            = new Wiring(actorSystem)
  private val redisFactory                      = new RedisFactory(redisClient, locationService, wiring)
  private val publisher                         = redisFactory.publisher().await
  private val subscriber                        = redisFactory.subscriber().await
  private val framework                         = new EventServicePubSubTestFramework(publisher, subscriber)

  override def afterAll(): Unit = {
    redisClient.shutdown()
    wiring.shutdown(TestFinishedReason).await
    redis.stop()
  }

  ignore("limiter") {
    framework.comparePerf(new RateLimiterStage(_))
  }

  ignore("adapter") {
    framework.comparePerf(new RateAdapterStage(_))
  }

  ignore("redis-throughput-latency") {
    framework.monitorPerf()
  }

  test("Redis pub sub") {
    framework.pubSub()
  }

  test("Redis independent subscriptions") {
    framework.subscribeIndependently()
  }

  ignore("Redis multiple publish") {
    framework.publishMultiple()
  }

  test("Redis retrieve recently published event on subscription") {
    framework.retrieveRecentlyPublished()
  }

  test("Redis retrieveInvalidEvent") {
    framework.retrieveInvalidEvent()
  }

  test("Redis get") {
    framework.get()
  }

  test("Redis get retrieveInvalidEvent") {
    framework.retrieveInvalidEventOnget()
  }

}
