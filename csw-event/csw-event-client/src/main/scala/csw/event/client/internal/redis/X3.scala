package csw.event.client.internal.redis
import io.bullet.borer.derivation.ArrayBasedCodecs._
import io.bullet.borer.{Cbor, Codec}
import upickle.default.{macroRW, ReadWriter => RW}

/*
Msgpack works pretty well with strings. It compresses a lot.
 While encoding time taken by cbor is quite less
 Decoding msgpk is faster in case of strings, for bytes its similar
 */
object X3 extends App {

  case class Person(name: Array[Byte])
//  case class Person(name: String)

//  private val person = Person("X" * (100 * 1000))
  private val person = Person(("X" * (100 * 1000)).getBytes)

  object Person {
    implicit val rw: RW[Person] = macroRW
  }

  println("********** MsgPack **************")
  val begin1 = System.currentTimeMillis()
  val dd1    = upickle.default.writeBinary(person)

  val p = upickle.default.readBinary[ujson.Obj](dd1)
//  val p3 = upickle.default.readBinary(dd1)
  println("time=" + (System.currentTimeMillis() - begin1))
  println("size=" + dd1.length) //how to find
  println(p)
//  println(p3)

  implicit val personCodec: Codec[Person] = deriveCodec[Person]

  println("********** Cbor **************")

  val begin2  = System.currentTimeMillis()
  val dd2     = Cbor.encode(person).toByteArray
  val (p2, _) = Cbor.decode(dd2).to[Person].getOrElse(throw new RuntimeException(""))
  println("time=" + (System.currentTimeMillis() - begin2))
  println("size=" + dd2.length)

}
