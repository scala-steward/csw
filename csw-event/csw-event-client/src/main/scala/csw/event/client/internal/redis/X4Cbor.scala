//package csw.event.client.internal.redis
//import java.time.Instant
//
//import csw.event.client.internal.redis.X2.event
//import csw.params.core.generics.{KeyType, Parameter}
//import csw.params.core.models.{Id, Prefix, Units}
//import csw.params.events.{Event, EventName, ObserveEvent, SystemEvent}
//import csw.time.core.models.{TAITime, TMTTime, UTCTime}
//import enumeratum.{Enum, EnumEntry}
//import io.bullet.borer.{Cbor, Codec, Decoder, Encoder}
//import play.api.libs.json.Format
//
//import scala.collection.mutable
//import scala.reflect.ClassTag
//
//object X4Cbor {
//  case class TS(seconds: Long, nanos: Int)
//
//  println("*********** CBOR ************")
//
//  import io.bullet.borer.derivation.ArrayBasedCodecs._
//
//  implicit val idCodec     = deriveCodec[Id]
//  implicit val prefixCodec = deriveCodec[Prefix]
//  implicit val nameCodec   = deriveCodec[EventName]
//
//  implicit val tsCodec = deriveCodec[TS]
//  implicit val insEnc  = tsCodec.encoder.compose[Instant](x => TS(x.getEpochSecond, x.getNano))
//  implicit val insDec  = tsCodec.decoder.map[Instant](x => Instant.ofEpochSecond(x.seconds, x.nanos))
//
//  implicit val utcCodec = deriveCodec[UTCTime]
//  implicit val taiCodec = deriveCodec[TAITime]
//  implicit val tmtCodec = deriveCodec[TMTTime]
//
//  def enumEnc[T <: EnumEntry]: Encoder[T]       = Encoder.forString.compose[T](_.entryName)
//  def enumDec[T <: EnumEntry: Enum]: Decoder[T] = Decoder.forString.map[T](implicitly[Enum[T]].withNameInsensitive)
//
//  implicit val unitsCodec = Codec(enumEnc[Units], enumDec[Units])
//  //  implicit val keytypeCodec2: Codec[KeyType[_]]  =
//  implicit def keytypeCodec[S: Format: ClassTag]: Codec[KeyType[S]] =
//    Codec(enumEnc[KeyType[_]], enumDec[KeyType[_]]).asInstanceOf[Codec[KeyType[S]]]
//
//  implicit def setEnc[T <: AnyRef: Encoder: ClassTag] = Encoder.forArray[T].compose[Set[T]](_.toArray)
//  implicit def setDec[T <: AnyRef: Decoder: ClassTag] = Decoder.forArray[T].map[Set[T]](_.toSet[T])
//
//  implicit def wArEnc[T <: AnyRef: Encoder]: Encoder[mutable.WrappedArray[T]] =
//    Encoder.forArray[T].compose[mutable.WrappedArray[T]](_.array)
//  implicit def wArDec[T <: AnyRef: Decoder: ClassTag]: Decoder[mutable.WrappedArray[T]] =
//    Decoder.forArray[T].map[mutable.WrappedArray[T]](d => d)
//
//  implicit def pCodec2[T <: AnyRef: Codec: Format: ClassTag]() = deriveCodec[Parameter[T]]
////  implicit val pCodec: Codec[Parameter[_]]                     = deriveCodec[Parameter[_]]
//
//  implicit val seCodec = deriveCodec[SystemEvent]
//  implicit val oeCodec = deriveCodec[ObserveEvent]
//  implicit val eCodec  = deriveCodec[Event]
//
//  val cbor = {
//    val begin = System.currentTimeMillis()
//    val bytes = Cbor.encode(event).toByteArray
//    println(System.currentTimeMillis() - begin)
//    println(bytes.length)
//  }
//}
