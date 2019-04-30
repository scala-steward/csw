package csw.benchmark.event

import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import csw.params.core.generics.Parameter
import csw.params.events.SystemEvent
import csw.params.javadsl.JKeyType
import io.bullet.borer.Cbor
import org.openjdk.jmh.annotations._

// RUN using this command: csw-benchmark/jmh:run -f 1 -wi 5 -i 5 csw.benchmark.event.CborSerializationBenchmark

import csw.params.core.formats.CborSupport._
@State(Scope.Benchmark)
class CborSerializationBenchmark {

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def cborAvgTime(): SystemEvent = {
    val bytes: Array[Byte] = Cbor.encode(Data.event).toByteArray
    Cbor.decode(bytes).to[SystemEvent].value
  }

}

object BigCborTest extends App {
  val bytes: Array[Byte] = Cbor.encode(Data.event).toByteArray
  val event: SystemEvent = Cbor
    .decode(bytes)
//    .withPrintLogging()
    .to[SystemEvent]
    .value
  assert(event == Data.event)
  println(bytes.length)
}

object SimpleCborTest extends App {
  private val jintKey                          = JKeyType.ByteKey.make("ints")
  private val param: Parameter[java.lang.Byte] = jintKey.set("abc".getBytes().map(x ⇒ x: java.lang.Byte))
  val bytes: Array[Byte]                       = Cbor.encode(param).toByteArray

  val result = Cbor
    .decode(bytes)
    .withPrintLogging()
    .to[Parameter[_]]
    .value
    .asInstanceOf[Parameter[Byte]]

  println(result)
}

object InterOpTest extends App {
  private val ints       = Array(1, 2, 3)
  val bytes: Array[Byte] = Cbor.encode(ints).toByteArray

  val result = Cbor
    .decode(bytes)
    .withPrintLogging()
    .to[Array[Integer]]
    .value

  println(result)
}

object CrossLanguageCbor extends App {
  val bytes: Array[Byte] = Cbor.encode(Data.smallEvent).toByteArray

  val bos = new BufferedOutputStream(new FileOutputStream("/tmp/input.cbor"))
  bos.write(bytes)
  bos.close()

  val readBytes = Files.readAllBytes(Paths.get("/tmp/input.cbor"))
  val event     = Cbor.decode(readBytes).to[SystemEvent].value
  println(event)
}
