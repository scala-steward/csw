package spikes

import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.file.{Files, Paths}

import com.google.protobuf.ByteString
import io.bullet.borer.{Cbor, Codec}
import proto.blob.PbBlob
import proto.inner_object.PbInnerObject
import scalapb.TypeMapper
//import io.bullet.borer.derivation.MapBasedCodecs._
import io.bullet.borer.derivation.ArrayBasedCodecs._

import upickle.default._

case class Blob(
    bytes: Array[Byte],
    ints: Array[Int],
    floats: Array[Float],
    doubles: Array[Double],
    strings: Seq[String],
    innerObjects: Array[InnerObject],
)

case class InnerObject(ra: Double, dec: Double)

object InnerObject {
  implicit val iOTypeMapper: TypeMapper[PbInnerObject, InnerObject] = new TypeMapper[PbInnerObject, InnerObject] {
    override def toCustom(base: PbInnerObject): InnerObject = InnerObject(base.ra, base.dec)
    override def toBase(custom: InnerObject): PbInnerObject = PbInnerObject(custom.ra, custom.dec)
  }
}

object Blob {

  val n = 100
  val data = Blob(
    Array(100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100),
    Array(100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100),
    Array(100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100),
    Array(100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100),
    Array("100", "100", "100", "100", "100", "100", "100", "100", "100", "100", "100", "100", "100", "100"),
    Array(InnerObject(100, 100), InnerObject(100, 100), InnerObject(100, 100), InnerObject(100, 100)),
  )

//  implicit val dd: Encoder[Integer]  = Encoder.forInt.compose[Integer](x => { x: Integer })
//  implicit val dd2: Decoder[Integer] = Decoder.forInt.map[Integer](x => { x: Int })
//  implicit val intArrEnc: Encoder[Array[Int]] = Encoder.forArray[Integer].compose[Array[Int]](_.map(x => x: Integer))
//  implicit val intArrDec: Decoder[Array[Int]] = Decoder.forArray[Integer].map[Array[Int]](_.map(x => x: Int))

  implicit val codec: Codec[InnerObject] = deriveCodec[InnerObject]
  implicit val codec2: Codec[Blob]       = deriveCodec[Blob]

  implicit val innRW: ReadWriter[InnerObject] = macroRW
  implicit val blobRW: ReadWriter[Blob]       = macroRW

  import InnerObject._

  val blobTypeMapper: TypeMapper[PbBlob, Blob] = new TypeMapper[PbBlob, Blob] {
    override def toCustom(base: PbBlob): Blob =
      Blob(base.bytes.toByteArray,
           base.ints,
           base.floats,
           base.doubles,
           base.strings,
           base.innerObjects.map(iOTypeMapper.toCustom))

    override def toBase(custom: Blob): PbBlob = {
      PbBlob(ByteString.copyFrom(custom.bytes),
             custom.ints,
             custom.floats,
             custom.doubles,
             custom.strings,
             custom.innerObjects.map(iOTypeMapper.toBase))
    }
  }
}

object Demo {

  def main(args: Array[String]): Unit = {
    println("************************ MsgPk *************************")

    (1 to 10).foreach { _ =>
      val begin: Long = System.nanoTime()
      val bytes       = write(Blob.data)
      val blob: Blob  = read[Blob](bytes)
      println("time=" + (System.nanoTime() - begin) / 1000) //in micros
      println("size---------------->" + bytes.length)

    //      println(upack.read(bytes).obj.foreach(println))
    //      println(readBinary[ujson.Obj](bytes).obj.foreach(println))
    }

    println("************************ Cbor *************************")

    (1 to 10).foreach { _ =>
      val begin: Long        = System.nanoTime()
      val bytes: Array[Byte] = Cbor.encode(Blob.data).toByteArray
      val blob: Blob         = Cbor.decode(bytes).to[Blob].value
      println("time=" + (System.nanoTime() - begin) / 1000) //in micros
      println("size---------------->" + bytes.length)
    }

    println("************************ PB *************************")
    (1 to 10).foreach { _ =>
      val begin: Long       = System.nanoTime()
      val bytes: ByteString = Blob.blobTypeMapper.toBase(Blob.data).toByteString
      val blob: Blob        = Blob.blobTypeMapper.toCustom(PbBlob(bytes))
      println("time=" + (System.nanoTime() - begin) / 1000) //in micros
      println("size---------------->" + bytes.size())
    }

    def cborFile(): Unit = {
      val bytes: Array[Byte] = Cbor.encode(Blob.data).toByteArray

      val bos = new BufferedOutputStream(new FileOutputStream("/tmp/input.cbor"))
      bos.write(bytes)
      bos.close()

      val readBytes = Files.readAllBytes(Paths.get("/tmp/input.cbor"))
      val blob      = Cbor.decode(readBytes).to[Blob].value
      println(blob)
    }

//    cborFile()
  }
}
