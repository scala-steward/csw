package csw.params.core.formats
import java.lang
import java.time.Instant

import com.github.ghik.silencer.silent
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models._
import csw.params.events.{Event, EventName, ObserveEvent, SystemEvent}
import csw.time.core.models.{TAITime, TMTTime, UTCTime}
import enumeratum.{Enum, EnumEntry}
import io.bullet.borer.{Codec, Decoder, Encoder, Writer}
import play.api.libs.json.Format

import scala.collection.mutable
import scala.reflect.ClassTag

object CborFormats {
  case class TS(seconds: Long, nanos: Int)

  import io.bullet.borer.derivation.ArrayBasedCodecs._
  import Encoder._
  import Decoder._
  implicit def cd[T: Encoder: Decoder] = Codec(implicitly[Encoder[T]], implicitly[Decoder[T]])

  implicit val idCodec: Codec[Id]          = deriveCodec[Id]
  implicit val prefixCodec: Codec[Prefix]  = deriveCodec[Prefix]
  implicit val nameCodec: Codec[EventName] = deriveCodec[EventName]

  implicit val tsCodec: Codec[TS]       = deriveCodec[TS]
  implicit val insEnc: Encoder[Instant] = tsCodec.encoder.compose[Instant](x => TS(x.getEpochSecond, x.getNano))
  implicit val insDec: Decoder[Instant] = tsCodec.decoder.map[Instant](x => Instant.ofEpochSecond(x.seconds, x.nanos))

  implicit val utcCodec: Codec[UTCTime] = deriveCodec[UTCTime]
  implicit val taiCodec: Codec[TAITime] = deriveCodec[TAITime]
  implicit val tmtCodec: Codec[TMTTime] = deriveCodec[TMTTime]

  def enumEnc[T <: EnumEntry]: Encoder[T]       = Encoder.forString.compose[T](_.entryName)
  def enumDec[T <: EnumEntry: Enum]: Decoder[T] = Decoder.forString.map[T](implicitly[Enum[T]].withNameInsensitive)

  implicit val unitsCodec: Codec[Units] = Codec(enumEnc[Units], enumDec[Units])

  implicit def keytypeCodec[S: ClassTag]: Codec[KeyType[S]] =
    Codec(enumEnc[KeyType[_]], enumDec[KeyType[_]]).asInstanceOf[Codec[KeyType[S]]]

//  implicit def keytypeEnc[S: Format: ClassTag]: Encoder[KeyType[S]] = enumEnc[KeyType[S]]
//
//  implicit def keytypeDec[S: Format: ClassTag]: Decoder[KeyType[S]] = enumDec[KeyType[S]]

  implicit def setEnc[T <: AnyRef: Encoder: ClassTag]: Encoder[Set[T]] = Encoder.forArray[T].compose[Set[T]](_.toArray)
  implicit def setDec[T <: AnyRef: Decoder: ClassTag]: Decoder[Set[T]] = Decoder.forArray[T].map[Set[T]](_.toSet[T])

  implicit def wArEnc[T <: AnyRef: Encoder]: Encoder[mutable.WrappedArray[T]] =
    Encoder.forArray[T].compose[mutable.WrappedArray[T]](_.array)

  implicit def wArDec[T <: AnyRef: Decoder]: Decoder[mutable.WrappedArray[T]] =
    Decoder.forArray[T].map[mutable.WrappedArray[T]](d => d)

//  implicit def wArEnc2[T <: AnyRef: Decoder: ClassTag]: Encoder[mutable.WrappedArray[_]] = new Encoder[mutable.WrappedArray[_]] {
//    override def write(w: Writer, value: mutable.WrappedArray[_]): w.type = w.write(value)
//  }

//  implicit def wArDec2[T <: AnyRef: Decoder: ClassTag]: Decoder[mutable.WrappedArray[_]] =
//    Decoder.forArray[T].map[mutable.WrappedArray[T]](d => d)

//  implicit val pCodec: Codec[Parameter[_]]                                        = deriveCodec[Parameter[_]]

  implicit def arrData[T <: AnyRef: Codec]: Codec[ArrayData[T]]                 = deriveCodec[ArrayData[T]]
  implicit def mtxData[T <: AnyRef: Codec]: Codec[MatrixData[T]]                = deriveCodec[MatrixData[T]]
  implicit val radecCodec: Codec[RaDec]                                         = deriveCodec[RaDec]
  implicit val choiceCodec: Codec[Choice]                                       = deriveCodec[Choice]
  implicit val structCodec: Codec[Struct]                                       = deriveCodec[Struct]
  implicit val strCodec                                                         = Codec(Encoder.forString, Decoder.forString)
  def formatFactory[S: Codec, J](implicit @silent conversion: S => J): Codec[J] = implicitly[Codec[S]].asInstanceOf[Codec[J]]

  implicit val booleanFormat: Codec[java.lang.Boolean]                            = formatFactory[Boolean, java.lang.Boolean]
  implicit val characterFormat: Codec[java.lang.Character]                        = formatFactory[Char, java.lang.Character]
  implicit val byteFormat: Codec[lang.Byte]                                       = formatFactory[Byte, java.lang.Byte]
  implicit val shortFormat: Codec[lang.Short]                                     = formatFactory[Short, java.lang.Short]
  implicit val longFormat: Codec[lang.Long]                                       = formatFactory[Long, java.lang.Long]
  implicit val integerFormat: Codec[lang.Integer]                                 = formatFactory[Int, java.lang.Integer]
  implicit val floatFormat: Codec[lang.Float]                                     = formatFactory[Float, java.lang.Float]
  implicit val doubleFormat: Codec[lang.Double]                                   = formatFactory[Double, java.lang.Double]
  implicit def pCodec2[T <: AnyRef: Codec: Format: ClassTag]: Codec[Parameter[T]] = deriveCodec[Parameter[T]]

  //  val enc: Encoder[Parameter[_]] = new Encoder[Parameter[_]] {
//    override def write(w: Writer, value: Parameter[_]): w.type = value.toCbor(w)
//  }

//  implicit val seCodec: Codec[SystemEvent]  = deriveCodec[SystemEvent]
//  implicit val oeCodec: Codec[ObserveEvent] = deriveCodec[ObserveEvent]
//  implicit val eCodec: Codec[Event]         = deriveCodec[Event]

}
