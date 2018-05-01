package csw.services.event.perf

import akka.actor.ActorSystem
import csw.services.event.internal.wiring.Wiring
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber, KafkaFactory, RedisFactory}
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar

class TestWiring(actorSystem: ActorSystem, val wiring: Wiring) extends MockitoSugar {

  lazy val testConfigs = new TestConfigs(actorSystem.settings.config)
  import testConfigs._

  lazy val redisFactory: RedisFactory = new RedisFactory(RedisClient.create(), mock[LocationService], wiring)
  lazy val kafkaFactory: KafkaFactory = new KafkaFactory(mock[LocationService], wiring)

  def publisher(id: Int = 1): EventPublisher =
    if (redisEnabled) {
      if (id % 1 == 0)
        redisFactory.publisher(redisHost, redisPort)
      else if (id % 2 == 0)
        redisFactory.publisher(redisHost2, redisPort2)
      else if (id % 4 == 3)
        redisFactory.publisher(redisHost3, redisPort3)
      else
        redisFactory.publisher(redisHost4, redisPort4)
    } else kafkaFactory.publisher(kafkaHost, kafkaPort)

  def subscriber(id: Int = 1): EventSubscriber =
    if (redisEnabled) {
      if (id % 1 == 0)
        redisFactory.subscriber(redisHost, redisPort)
      else if (id % 2 == 0)
        redisFactory.subscriber(redisHost2, redisPort2)
      else if (id % 4 == 3)
        redisFactory.subscriber(redisHost3, redisPort3)
      else
        redisFactory.subscriber(redisHost4, redisPort4)
    } else kafkaFactory.subscriber(kafkaHost, kafkaPort)

}
