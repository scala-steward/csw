package server

import java.util.Properties

import com.lightbend.kafka.scala.streams.{KTableS, StreamsBuilderS}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.{Consumed, KafkaStreams, Topology}
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.{KeyValueStore, QueryableStoreTypes, ReadOnlyKeyValueStore}

object AlarmStore extends App {
  val streamsConfiguration = new Properties()

  import org.apache.kafka.streams.StreamsConfig._

  streamsConfiguration.put(APPLICATION_ID_CONFIG, "Streaming-QuickStart")
  private val dd = 100
  //  streamsConfiguration.put(POLL_MS_CONFIG, dd)
  streamsConfiguration.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  streamsConfiguration.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass.getName)
  streamsConfiguration.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass.getName)
  streamsConfiguration.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
  streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
  implicit val consumed: Consumed[String, String] = Consumed.`with`(Topology.AutoOffsetReset.LATEST)

  val streamBuilder = new StreamsBuilderS
//  streamBuilder.stream("alarm.nfiraos.cc.trombone.tromboneAxisLowLimitAlarm.state").foreach((k, v) ⇒ println(s" $k -- $v"))

  val materialized: Materialized[String, String, KeyValueStore[Bytes, Array[Byte]]] =
    Materialized.as("LatestAlarmStateStore")

  private val kTable: KTableS[String, String] =
    streamBuilder.table("alarm.nfiraos.cc.trombone.tromboneAxisLowLimitAlarm.state", materialized)(consumed)

  println(kTable.queryableStoreName)

  kTable.toStream.foreach((k, v) ⇒ println(s"$k -- $v"))

  val streams = new KafkaStreams(streamBuilder.build(), streamsConfiguration)
  streams.start()

  Thread.sleep(6000)

  val keyValueStore: ReadOnlyKeyValueStore[String, String] =
    streams.store("LatestAlarmStateStore", QueryableStoreTypes.keyValueStore[String, String]())

  val range = keyValueStore.all
  while (range.hasNext) {
    val next = range.next
    println(s"count for ${next.key} : ${next.value}")
  }
}
