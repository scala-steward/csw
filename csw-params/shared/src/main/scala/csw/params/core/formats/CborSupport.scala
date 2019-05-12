package csw.params.core.formats

import java.lang.{Byte => JByte}
import java.time.Instant

import com.github.ghik.silencer.silent
import csw.params.commands._
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models._
import csw.params.events.{Event, EventName, ObserveEvent, SystemEvent}
import csw.time.core.models.{TAITime, UTCTime}
import enumeratum._
import io.bullet.borer._
import io.bullet.borer.derivation.MapBasedCodecs._

import scala.collection.mutable.{WrappedArray => WArray}
import scala.reflect.ClassTag

@silent
object CborSupport {

  type ArrayEnc[T] = Encoder[Array[T]]
  type ArrayDec[T] = Decoder[Array[T]]

  private def bimap[From: Encoder: Decoder, To](to: From ⇒ To, from: To ⇒ From): Codec[To] = Codec(
    implicitly[Encoder[From]].contramap(from),
    implicitly[Decoder[From]].map(to)
  )

  // ************************ Base Type Codecs ********************

  //Ensure that Codec.forCaseClass is used ONLY for unary case classes see https://github.com/sirthias/borer/issues/26
  implicit lazy val choiceCodec: Codec[Choice] = Codec.forCaseClass[Choice]
  implicit lazy val raDecCodec: Codec[RaDec]   = deriveCodec[RaDec]

  implicit lazy val tsCodec: Codec[Timestamp]    = deriveCodec[Timestamp]
  implicit lazy val instantCodec: Codec[Instant] = bimap[Timestamp, Instant](_.toInstant, Timestamp.fromInstant)

  implicit lazy val utcTimeCodec: Codec[UTCTime] = Codec.forCaseClass[UTCTime]
  implicit lazy val taiTimeCodec: Codec[TAITime] = Codec.forCaseClass[TAITime]

  // ************************ Composite Codecs ********************

  implicit def arrayDataEnc[T: ClassTag: ArrayEnc: ArrayDec]: Codec[ArrayData[T]] =
    bimap[WArray[T], ArrayData[T]](ArrayData(_), _.data)

  implicit def matrixDataEnc[T: ClassTag: ArrayEnc: ArrayDec]: Codec[MatrixData[T]] =
    bimap[WArray[WArray[T]], MatrixData[T]](MatrixData(_), _.data)

  // ************************ Enum Codecs ********************

  def enumCodec[T <: EnumEntry: Enum]: Codec[T] = bimap[String, T](implicitly[Enum[T]].withNameInsensitive, _.entryName)

  implicit lazy val unitsCodec: Codec[Units]                   = enumCodec[Units]
  implicit lazy val keyTypeCodecExistential: Codec[KeyType[_]] = enumCodec[KeyType[_]]
  implicit def keyTypeCodec[T]: Codec[KeyType[T]]              = keyTypeCodecExistential.asInstanceOf[Codec[KeyType[T]]]

  // ************************ Parameter Codecs ********************

  //Do not replace these with bimap, due to an issue with borer https://github.com/sirthias/borer/issues/24
  implicit val javaByteArrayEnc: Encoder[Array[JByte]] = Encoder.forByteArray.contramap(javaArray ⇒ javaArray.map(x ⇒ x: Byte))
  implicit val javaByteArrayDec: Decoder[Array[JByte]] = Decoder.forByteArray.map(scalaArray ⇒ scalaArray.map(x ⇒ x: JByte))

  implicit def waCodec[T: ClassTag: ArrayEnc: ArrayDec]: Codec[WArray[T]] =
    bimap[Array[T], WArray[T]](x => x: WArray[T], _.array)

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
    Parameter(keyName, keyType.asInstanceOf[KeyType[Any]], wa.asInstanceOf[WArray[Any]], units)
  }

  // ************************ Struct Codecs ********************

  implicit lazy val structCodec: Codec[Struct] = deriveCodec[Struct]

  // ************************ Simple Model Codecs ********************

  //Codec.forCaseClass does not work for id due to https://github.com/sirthias/borer/issues/23
  implicit lazy val idCodec: Codec[Id]         = bimap[String, Id](Id(_), _.id)
  implicit lazy val prefixCodec: Codec[Prefix] = Codec.forCaseClass[Prefix]

  // ************************ Event Codecs ********************

  implicit lazy val eventNameCodec: Codec[EventName] = Codec.forCaseClass[EventName]

  // this is done to ensure concrete type of event is encoded.
  implicit lazy val sysEventCodec: Codec[SystemEvent]  = bimap[Event, SystemEvent](_.asInstanceOf[SystemEvent], x ⇒ x: Event)
  implicit lazy val obsEventCodec: Codec[ObserveEvent] = bimap[Event, ObserveEvent](_.asInstanceOf[ObserveEvent], x ⇒ x: Event)

  implicit lazy val eventCodec: Codec[Event] = {
    implicit val seCodec: Codec[SystemEvent]  = deriveCodec[SystemEvent]
    implicit val oeCodec: Codec[ObserveEvent] = deriveCodec[ObserveEvent]
    deriveCodec[Event]
  }

  // ************************ Command Codecs ********************

  implicit lazy val commandNameCodec: Codec[CommandName] = Codec.forCaseClass[CommandName]
  implicit lazy val obsIdCodec: Codec[ObsId]             = Codec.forCaseClass[ObsId]

  implicit lazy val observeCommandCodec: Codec[Observe]          = deriveCodec[Observe]
  implicit lazy val setupCommandCodec: Codec[Setup]              = deriveCodec[Setup]
  implicit lazy val waitCommandCodec: Codec[Wait]                = deriveCodec[Wait]
  implicit lazy val sequenceCommandCodec: Codec[SequenceCommand] = deriveCodec[SequenceCommand]
  implicit lazy val controlCommandCodec: Codec[ControlCommand]   = deriveCodec[ControlCommand]
  implicit lazy val commandCodec: Codec[Command]                 = deriveCodec[Command]

}

case class Timestamp(seconds: Long, nanos: Long) {
  def toInstant: Instant = Instant.ofEpochSecond(seconds, nanos)

}

object Timestamp {
  def fromInstant(instant: Instant): Timestamp = Timestamp(instant.getEpochSecond, instant.getNano)
}
