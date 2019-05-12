package csw.benchmark.event

import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import csw.params.core.formats.EventCbor
import csw.params.events.SystemEvent
import org.openjdk.jmh.annotations._

// RUN using this command: csw-benchmark/jmh:run -f 1 -wi 5 -i 5 csw.benchmark.event.CborSerializationBenchmark
@State(Scope.Benchmark)
class CborSerializationBenchmark {

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def cborAvgTime(): SystemEvent = {
    val bytes: Array[Byte] = EventCbor.encode(Data.event)
    EventCbor.decode[SystemEvent](bytes)
  }

}

object BigCborTest extends App {
  val bytes: Array[Byte] = EventCbor.encode(Data.event)
  val event              = EventCbor.decode[SystemEvent](bytes)
  assert(event == Data.event)
  println(bytes.length)
}

object SmallCborTest extends App {
  val bytes: Array[Byte] = EventCbor.encode(Data.smallEvent)
  val event              = EventCbor.decode[SystemEvent](bytes)
  assert(event == Data.smallEvent)
  println(bytes.length)
}

object CrossLanguageCbor extends App {
  val bytes: Array[Byte] = EventCbor.encode(Data.smallEvent)

  val bos = new BufferedOutputStream(new FileOutputStream("/tmp/input.cbor"))
  bos.write(bytes)
  bos.close()

  val readBytes = Files.readAllBytes(Paths.get("/tmp/input.cbor"))
  val event     = EventCbor.decode[SystemEvent](readBytes)
  println(event)
}
