package csw.benchmark.event
import java.util.concurrent.TimeUnit

import io.bullet.borer.Cbor
import org.openjdk.jmh.annotations._
import proto.blob.PbBlob
import spikes.Blob
import upickle.default.{readBinary, writeBinary}
@State(Scope.Benchmark)
class SerializationBenchmark {

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime, Mode.SingleShotTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def cbor(): Blob = {
    val bytes: Array[Byte] = Cbor.encode(Blob.data).toByteArray
    Cbor.decode(bytes).to[Blob].value
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime, Mode.SingleShotTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def msgPc(): Blob = {
    val bytes = writeBinary(Blob.data)
    readBinary[Blob](bytes)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime, Mode.SingleShotTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def pb(): Blob = {
    val bytes: Array[Byte] = Blob.blobTypeMapper.toBase(Blob.data).toByteArray
    Blob.blobTypeMapper.toCustom(PbBlob.parseFrom(bytes))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def cborThrpt(): Blob = {
    val bytes: Array[Byte] = Cbor.encode(Blob.data).toByteArray
    Cbor.decode(bytes).to[Blob].value
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def msgPcThrpt(): Blob = {
    val bytes = writeBinary(Blob.data)
    readBinary[Blob](bytes)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def pbThrpt(): Blob = {
    val bytes: Array[Byte] = Blob.blobTypeMapper.toBase(Blob.data).toByteArray
    Blob.blobTypeMapper.toCustom(PbBlob.parseFrom(bytes))
  }
}
