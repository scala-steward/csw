package server

import java.util.Properties

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.lightbend.kafka.scala.streams.{KTableS, StreamsBuilderS}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.{Consumed, KafkaStreams, Topology}
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.{KeyValueStore, QueryableStoreTypes}

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object AlarmServer extends App {

  implicit val system: ActorSystem                        = ActorSystem("alarmserver")
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val streamsConfiguration = new Properties()

  import org.apache.kafka.streams.StreamsConfig._

  streamsConfiguration.put(APPLICATION_ID_CONFIG, "Streaming-QuickStart")
//  streamsConfiguration.put(POLL_MS_CONFIG, 100)
  streamsConfiguration.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  streamsConfiguration.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass.getName)
  streamsConfiguration.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass.getName)
  streamsConfiguration.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
  streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
  implicit val consumed: Consumed[String, String] = Consumed.`with`(Topology.AutoOffsetReset.LATEST)

  val streamBuilder = new StreamsBuilderS

  val materialized: Materialized[String, String, KeyValueStore[Bytes, Array[Byte]]] =
    Materialized.as("LatestAlarmStateStore")

  private val kTable: KTableS[String, String] =
    streamBuilder.table("alarm.nfiraos.cc.trombone.tromboneAxisLowLimitAlarm.state", materialized)(consumed)

  println(kTable.queryableStoreName)

  val streams = new KafkaStreams(streamBuilder.build(), streamsConfiguration)
  streams.start()

  Thread.sleep(6000) // wait till the stream starts successfully

  val keyValueStore = streams.store("LatestAlarmStateStore", QueryableStoreTypes.keyValueStore[String, String]())

  val route: Route = get {
    path("") {
      get {
        complete {
          keyValueStore.get("state")
        }
      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind()).onComplete { _ =>
    system.terminate()
  }
}
