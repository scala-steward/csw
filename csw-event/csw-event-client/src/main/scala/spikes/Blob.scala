package spikes

import io.bullet.borer.Dom.Element
import io.bullet.borer.{Cbor, Codec, Decoder, Encoder}

import io.bullet.borer.derivation.MapBasedCodecs._

//import io.bullet.borer.derivation.ArrayBasedCodecs._

import upickle.default._

case class Blob(
    bytes: Array[Byte],
    ints: Array[Int],
    floats: Array[Float],
    doubles: Array[Double],
    strings: Array[String],
    innerObjects: Array[InnerObject],
)

case class InnerObject(ra: Double, dec: Double)

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
}

object Demo {
  Blob.data

  def main(args: Array[String]): Unit = {
    locally {
      val begin: Long = System.currentTimeMillis()
      val bytes       = write(Blob.data)
      val blob: Blob  = read[Blob](bytes)
      println(System.currentTimeMillis() - begin)
      println(bytes.length)
//      println(upack.read(bytes).obj.foreach(println))
//      println(readBinary[ujson.Obj](bytes).obj.foreach(println))
    }

    locally {
      val begin: Long        = System.currentTimeMillis()
      val bytes: Array[Byte] = Cbor.encode(Blob.data).toByteArray
      val blob: Blob         = Cbor.decode(bytes).to[Blob].value
      println(System.currentTimeMillis() - begin)
      println(bytes.length)
      println(Cbor.decode(bytes).to[Element].value)
    }

  }

}
