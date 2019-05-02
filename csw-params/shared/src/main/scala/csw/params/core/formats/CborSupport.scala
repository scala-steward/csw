package csw.params.core.formats

import java.lang.{Byte ⇒ JByte}
import java.time.Instant

import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models._
import csw.params.events.{Event, EventName, ObserveEvent, SystemEvent}
import csw.time.core.models.{TAITime, UTCTime}
import enumeratum._
import io.bullet.borer._
import io.bullet.borer.derivation.MapBasedCodecs._

import scala.collection.mutable
import scala.reflect.ClassTag

object CborSupport {

  type ArrayEnc[T] = Encoder[Array[T]]
  type ArrayDec[T] = Decoder[Array[T]]

  def transform[A: Encoder: Decoder, B](to: A ⇒ B, from: B ⇒ A): Codec[B] = Codec(
    implicitly[Encoder[A]].compose(from),
    implicitly[Decoder[A]].map(to)
  )

  // ************************ Base Type Codecs ********************

  implicit lazy val choiceCodec: Codec[Choice] = Codec.forCaseClass[Choice]
  implicit lazy val raDecCodec: Codec[RaDec]   = Codec.forCaseClass[RaDec]

  implicit lazy val tsCodec: Codec[Timestamp] = deriveCodec[Timestamp]
  implicit lazy val insEnc: Encoder[Instant]  = Encoder.fromCodec[Timestamp].compose(Timestamp.fromInstant)
  implicit lazy val insDec: Decoder[Instant]  = Decoder.fromCodec[Timestamp].map(_.toInstant)

  implicit lazy val utcTimeCodec: Codec[UTCTime] = Codec.forCaseClass[UTCTime]
  implicit lazy val taiTimeCodec: Codec[TAITime] = Codec.forCaseClass[TAITime]

  // ************************ Composite Codecs ********************

  implicit def arrayDataEnc[T: ClassTag: ArrayEnc: ArrayDec]: Codec[ArrayData[T]]   = deriveCodec[ArrayData[T]]
  implicit def matrixDataEnc[T: ClassTag: ArrayEnc: ArrayDec]: Codec[MatrixData[T]] = deriveCodec[MatrixData[T]]

  // ************************ Enum Codecs ********************

  def enumEnc[T <: EnumEntry]: Encoder[T]       = Encoder.forString.compose[T](_.entryName)
  def enumDec[T <: EnumEntry: Enum]: Decoder[T] = Decoder.forString.map[T](implicitly[Enum[T]].withNameInsensitive)
  def enumCodec[T <: EnumEntry: Enum]: Codec[T] = Codec(enumEnc[T], enumDec[T])

  implicit lazy val unitsCodec: Codec[Units]                   = enumCodec[Units]
  implicit lazy val keyTypeCodecExistential: Codec[KeyType[_]] = enumCodec[KeyType[_]]
  implicit def keyTypeCodec[T]: Codec[KeyType[T]]              = keyTypeCodecExistential.asInstanceOf[Codec[KeyType[T]]]

  // ************************ Parameter Codecs ********************

  implicit val javaByteArrayEnc: Encoder[Array[JByte]] = Encoder.forByteArray.compose(javaArray ⇒ javaArray.map(x ⇒ x: Byte))
  implicit val javaByteArrayDec: Decoder[Array[JByte]] = Decoder.forByteArray.map(scalaArray ⇒ scalaArray.map(x ⇒ x: JByte))

  implicit def waEnc[T: ClassTag: ArrayEnc]: Encoder[mutable.WrappedArray[T]] = implicitly[ArrayEnc[T]].compose(_.array)
  implicit def waDec[T: ClassTag: ArrayDec]: Decoder[mutable.WrappedArray[T]] =
    implicitly[ArrayDec[T]].map(x => x: mutable.WrappedArray[T])

  implicit def paramCodec[T: ClassTag: ArrayEnc: ArrayDec]: Codec[Parameter[T]] = deriveCodec[Parameter[T]]

  implicit lazy val paramEncExistential: Encoder[Parameter[_]] = { (w: Writer, value: Parameter[_]) =>
    val encoder: Encoder[Parameter[Any]] = value.keyType.paramEncoder.asInstanceOf[Encoder[Parameter[Any]]]
    encoder.write(w, value.asInstanceOf[Parameter[Any]])
  }

  implicit lazy val paramDecExistential: Decoder[Parameter[_]] = { r: Reader =>
    r.tryReadMapHeader(4) || r.tryReadArrayHeader(4)
    r.tryReadString("keyName")
    val keyName = r.readString()
    r.tryReadString("keyType")
    val keyType = KeyType.withNameInsensitive(r.readString())
    r.tryReadString("items")
    val wa = keyType.waDecoder.read(r)
    r.tryReadString("units")
    val units = unitsCodec.decoder.read(r)
    Parameter(keyName, keyType.asInstanceOf[KeyType[Any]], wa.asInstanceOf[mutable.WrappedArray[Any]], units)
  }

  // ************************ Struct Codecs ********************

  implicit lazy val structCodec: Codec[Struct] = deriveCodec[Struct]

  // ************************ Event Codecs ********************

  implicit lazy val idCodec: Codec[Id]               = transform[String, Id](Id(_), _.id)
  implicit lazy val prefixCodec: Codec[Prefix]       = transform[String, Prefix](Prefix(_), _.prefix)
  implicit lazy val eventNameCodec: Codec[EventName] = transform[String, EventName](EventName(_), _.name)

  implicit lazy val sysEventCodec: Codec[SystemEvent]  = deriveCodec[SystemEvent]
  implicit lazy val obsEventCodec: Codec[ObserveEvent] = deriveCodec[ObserveEvent]
  implicit lazy val eventCodec: Codec[Event]           = deriveCodec[Event]
}

case class Timestamp(seconds: Long, nanos: Long) {
  def toInstant: Instant = Instant.ofEpochSecond(seconds, nanos)

}

object Timestamp {
  def fromInstant(instant: Instant): Timestamp = Timestamp(instant.getEpochSecond, instant.getNano)
}