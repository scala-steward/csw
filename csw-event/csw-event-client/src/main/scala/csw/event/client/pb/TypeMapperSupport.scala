package csw.event.client.pb

import java.time.Instant

import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models._
import csw.params.events.{Event, EventName, ObserveEvent, SystemEvent}
import csw.time.core.models.{TAITime, UTCTime}
import csw_protobuf.events.PbEvent
import csw_protobuf.events.PbEvent.PbEventType
import csw_protobuf.keytype.PbKeyType
import csw_protobuf.parameter._
import csw_protobuf.units.PbUnits
import play.api.libs.json.Format
import scalapb.TypeMapper

import scala.collection.mutable
import scala.reflect.ClassTag

object TypeMapperSupport {

  lazy val utcTimeTypeMapper: TypeMapper[PbUTCTime, UTCTime] = TypeMapper[PbUTCTime, UTCTime](
    x ⇒ UTCTime(Instant.ofEpochSecond(x.value.get.seconds, x.value.get.nanos))
  )(
    x ⇒ PbUTCTime(Some(Timestamp(x.value.getEpochSecond, x.value.getNano)))
  )

  lazy val paramValueMapper: TypeMapper[PbParamValue, Any] = TypeMapper[PbParamValue, Any] {
    case PbInt(value)     => value
    case PbLong(value)    => value
    case PbFloat(value)   => value
    case PbShort(value)   => value
    case PbDouble(value)  => value
    case PbChar(value)    => value
    case PbBoolean(value) => value
    case PbString(value)  => value
    case PbChoice(value)  => Choice(value)
    case PbRaDec(ra, dec) => RaDec(ra, dec)
    case PbTAITime(value) => TAITime(Instant.ofEpochSecond(value.get.seconds, value.get.nanos))
    case value: PbUTCTime => utcTimeTypeMapper.toCustom(value)
    case PbArray(xs)      => ArrayData(xs.map(paramValueMapper.toCustom).to[mutable.WrappedArray])
    case PbMatrix(xss) =>
      MatrixData(
        xss
          .map(xs => paramValueMapper.toCustom(xs).asInstanceOf[ArrayData[Any]].values.to[mutable.WrappedArray])
          .to[mutable.WrappedArray]
      )
    case PbStruct(xs) => Struct(xs.map(x => paramMapper.toCustom(x)).toSet)
    case x            => throw new RuntimeException(s"can not decode paramValue=$x")
  } {
    case x: Int          => PbInt(x)
    case x: Long         => PbLong(x)
    case x: Float        => PbFloat(x)
    case x: Short        => PbShort(x)
    case x: Double       => PbDouble(x)
    case x: Char         => PbChar(x)
    case x: Boolean      => PbBoolean(x)
    case x: String       => PbString(x)
    case Choice(value)   => PbChoice(value)
    case RaDec(ra, dec)  => PbRaDec(ra, dec)
    case x: UTCTime      => utcTimeTypeMapper.toBase(x)
    case TAITime(value)  => PbTAITime(Some(Timestamp(value.getEpochSecond, value.getNano)))
    case ArrayData(xs)   => PbArray().withValues(xs.map(paramValueMapper.toBase))
    case MatrixData(xss) => PbMatrix().withValues(xss.map(xs => paramValueMapper.toBase(ArrayData(xs)).asInstanceOf[PbArray]))
    case Struct(xs)      => PbStruct().withParamSet(xs.map(x => paramMapper.toBase(x.asInstanceOf[Parameter[Any]])).toSeq)
    case x               => throw new RuntimeException(s"can not encode parameterValue=$x")
  }

  lazy val paramMapper: TypeMapper[PbParameter, Parameter[_]] = TypeMapper[PbParameter, Parameter[_]](
    pbParameter =>
      Parameter(
        pbParameter.name,
        pbParameter.keyType.asInstanceOf[KeyType[Any]],
        decodeParamValues(pbParameter).asInstanceOf[mutable.WrappedArray[Any]],
        pbParameter.units
      )(pbParameter.keyType.sFormat.asInstanceOf[Format[Any]], pbParameter.keyType.sClassTag.asInstanceOf[ClassTag[Any]])
  )(
    parameter =>
      PbParameter()
        .withName(parameter.keyName)
        .withKeyType(parameter.keyType)
        .withUnits(parameter.units)
        .withValues(encodeParamValues(parameter))
  )

  private def encodeParamValues(parameter: Parameter[_]): Seq[PbParamValue] = {
    if (parameter.items.nonEmpty && parameter.items.head.isInstanceOf[Byte]) {
      val bytes = parameter.items.asInstanceOf[mutable.WrappedArray[Byte]]
      Array(PbBytes(ByteString.copyFrom(bytes.array)))
    } else {
      parameter.items.map(paramValueMapper.toBase)
    }
  }

  private def decodeParamValues(pbParameter: PbParameter): mutable.WrappedArray[_] = {
    if (pbParameter.values.nonEmpty && pbParameter.values.head.isInstanceOf[PbBytes]) {
      pbParameter.values.head.asInstanceOf[PbBytes].toByteArray
    } else {
      pbParameter.values.map(paramValueMapper.toCustom).to[mutable.WrappedArray]
    }
  }

  private[csw] implicit def eventTypeMapper[T <: Event]: TypeMapper[PbEvent, T] = new TypeMapper[PbEvent, T] {
    override def toCustom(base: PbEvent): T = {
      val factory: (Id, Prefix, EventName, UTCTime, Set[Parameter[_]]) ⇒ Any = base.eventType match {
        case PbEventType.SystemEvent     ⇒ SystemEvent.apply
        case PbEventType.ObserveEvent    ⇒ ObserveEvent.apply
        case PbEventType.Unrecognized(x) ⇒ throw new RuntimeException(s"unknown event type=[${base.eventType.toString} :$x]")
      }

      factory(
        Id(base.eventId),
        Prefix(base.source),
        EventName(base.name),
        base.eventTime.map(utcTimeTypeMapper.toCustom).get,
        base.paramSet.map(paramMapper.toCustom).toSet
      ).asInstanceOf[T]
    }

    override def toBase(custom: T): PbEvent = {
      val pbEventType = custom match {
        case _: ObserveEvent ⇒ PbEventType.ObserveEvent
        case _: SystemEvent  ⇒ PbEventType.SystemEvent
      }
      PbEvent()
        .withEventId(custom.eventId.id)
        .withSource(custom.source.prefix)
        .withName(custom.eventName.name)
        .withEventTime(utcTimeTypeMapper.toBase(custom.eventTime))
        .withParamSet(custom.paramSet.map(x => paramMapper.toBase(x)).toSeq)
        .withEventType(pbEventType)
    }
  }

  implicit val unitsTypeMapper: TypeMapper[PbUnits, Units] =
    TypeMapper[PbUnits, Units](x ⇒ Units.withName(x.toString()))(x ⇒ PbUnits.fromName(x.toString).get)

  implicit val keyTypeTypeMapper: TypeMapper[PbKeyType, KeyType[_]] =
    TypeMapper[PbKeyType, KeyType[_]](x ⇒ KeyType.withName(x.toString()))(x ⇒ PbKeyType.fromName(x.toString).get)

}
