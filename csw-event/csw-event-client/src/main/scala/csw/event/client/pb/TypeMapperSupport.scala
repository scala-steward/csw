//package csw.event.client.pb
//
//import com.google.protobuf.ByteString
//import csw.event.client.pb.TypeMapperFactory.make
//import csw.params.core.generics.{KeyType, Parameter}
//import csw.params.core.models.ObsId.empty
//import csw.params.core.models._
//import csw.params.events._
//import csw.time.core.models.{TAITime, UTCTime}
//import csw_protobuf.events.PbEvent
//import csw_protobuf.events.PbEvent.PbEventType
//import csw_protobuf.keytype.PbKeyType
//import csw_protobuf.parameter._
//import csw_protobuf.primitives._
//import csw_protobuf.units.PbUnits
//import play.api.libs.json.Format
//import scalapb.TypeMapper
//
//import scala.collection.mutable
//import scala.reflect.ClassTag
//
//object TypeMapperSupport {
//
//  implicit def parameterTypeMapper[P <: PbParamValue, S: ClassTag: Format](
//      implicit typeMapper: TypeMapper[P, S]
//  ): TypeMapper[PbParameter, Parameter[S]] =
//    new TypeMapper[PbParameter, Parameter[S]] {
//      override def toCustom(pbParameter: PbParameter): Parameter[S] = Parameter(
//        pbParameter.name,
//        pbParameter.keyType.asInstanceOf[KeyType[S]],
//        cswItems(pbParameter.values),
//        pbParameter.units
//      )
//
//      override def toBase(x: Parameter[S]): PbParameter =
//        PbParameter()
//          .withName(x.keyName)
//          .withUnits(x.units)
//          .withKeyType(x.keyType)
//          .withValues(x.items.map(d => encodeParamValue(d)))
//    }
//
//  implicit val parameterTypeMapper2: TypeMapper[PbParameter, Parameter[_]] = {
//    TypeMapper[PbParameter, Parameter[_]](
//      p ⇒ make(p.keyType).toCustom(p)
//    )(p => make(p.keyType).toBase(p))
//  }
//
//  private def cswItems[T: ClassTag](items: Seq[PbParamValue]): mutable.WrappedArray[T] =
//    items.map(x => decodeParamValue(x).asInstanceOf[T]).to[mutable.WrappedArray]
//
//  private val ParameterSetMapper =
//    TypeMapper[Seq[PbParameter], Set[Parameter[_]]] {
//      _.map(TypeMapperSupport.parameterTypeMapper2.toCustom).toSet
//    } {
//      _.map(TypeMapperSupport.parameterTypeMapper2.toBase).toSeq
//    }
//
//  private[csw] implicit def eventTypeMapper[T <: Event]: TypeMapper[PbEvent, T] = new TypeMapper[PbEvent, T] {
//    override def toCustom(base: PbEvent): T = {
//      val factory: (Id, Prefix, EventName, UTCTime, Set[Parameter[_]]) ⇒ Any = base.eventType match {
//        case PbEventType.SystemEvent     ⇒ SystemEvent.apply
//        case PbEventType.ObserveEvent    ⇒ ObserveEvent.apply
//        case PbEventType.Unrecognized(x) ⇒ throw new RuntimeException(s"unknown event type=[${base.eventType.toString} :$x]")
//      }
//
//      factory(
//        Id(base.eventId),
//        Prefix(base.source),
//        EventName(base.name),
//        base.eventTime.map(utcTimeTypeMapper.toCustom).get,
//        ParameterSetMapper.toCustom(base.paramSet)
//      ).asInstanceOf[T]
//    }
//
//    override def toBase(custom: T): PbEvent = {
//      val pbEventType = custom match {
//        case _: ObserveEvent ⇒ PbEventType.ObserveEvent
//        case _: SystemEvent  ⇒ PbEventType.SystemEvent
//      }
//      PbEvent()
//        .withEventId(custom.eventId.id)
//        .withSource(custom.source.prefix)
//        .withName(custom.eventName.name)
//        .withEventTime(utcTimeTypeMapper.toBase(custom.eventTime))
//        .withParamSet(ParameterSetMapper.toBase(custom.paramSet))
//        .withEventType(pbEventType)
//    }
//  }
//
//  implicit val structTypeMapper: TypeMapper[PbStruct, Struct] = TypeMapper[PbStruct, Struct] { s =>
//    Struct(s.paramSet.map(parameterTypeMapper2.toCustom).toSet)
//  } { s =>
//    PbStruct().withParamSet(s.paramSet.map(TypeMapperSupport.parameterTypeMapper2.toBase).toSeq)
//  }
//
//  implicit val choiceTypeMapper: TypeMapper[PbChoice, Choice] = TypeMapper[PbChoice, Choice](
//    p => Choice(p.value.name)
//  )(x => PbChoice(x.name))
//
//  implicit val stringTypeMapper: TypeMapper[PbString, String] = TypeMapper[PbString, String](_.value)(x => PbString(x))
//  implicit val intTypeMapper: TypeMapper[PbInt, Int]          = TypeMapper[PbInt, Int](_.value)(x => PbInt(x))
//  implicit val shortTypeMapper: TypeMapper[PbShort, Short]    = TypeMapper[PbShort, Short](_.value.toShort)(x => PbShort(x))
//  implicit val longTypeMapper: TypeMapper[PbLong, Long]       = TypeMapper[PbLong, Long](_.value)(x => PbLong(x))
//  implicit val floatTypeMapper: TypeMapper[PbFloat, Float]    = TypeMapper[PbFloat, Float](_.value)(x => PbFloat(x))
//  implicit val doubleTypeMapper: TypeMapper[PbDouble, Double] = TypeMapper[PbDouble, Double](_.value)(x => PbDouble(x))
//  implicit val charTypeMapper: TypeMapper[PbChar, Char]       = TypeMapper[PbChar, Char](_.value.toChar)(x => PbChar(x))
//  implicit val bytesTypeMapper: TypeMapper[PbBytes, Byte] =
//    TypeMapper[PbBytes, Byte](_.value.toByteArray.head)(x => PbBytes(ByteString.copyFrom(scala.Array(x))))
//  implicit val boolTypeMapper: TypeMapper[PbBoolean, Boolean] = TypeMapper[PbBoolean, Boolean](_.value)(x => PbBoolean(x))
//
//  implicit val unitsTypeMapper: TypeMapper[PbUnits, Units] =
//    TypeMapper[PbUnits, Units](x ⇒ Units.withName(x.toString()))(x ⇒ PbUnits.fromName(x.toString).get)
//
//  implicit def matrixDataTypeMapper[T: ClassTag]: TypeMapper[PbMatrix, MatrixData[T]] =
//    TypeMapper[PbMatrix, MatrixData[T]](
//      x ⇒ MatrixData.fromArrays(x.values.toArray.map(a ⇒ a.values.map(decodePrimitive).toArray[T]))
//    )(
//      x ⇒ PbMatrix().withValues(x.data.map(a => PbArray().withValues(a.map(encodePrimitive))))
//    )
//
//  implicit val raDecTypeMapper: TypeMapper[PbRaDec, RaDec] =
//    TypeMapper[PbRaDec, RaDec] { case PbRaDec(Some(x)) ⇒ x }(x ⇒ PbRaDec().withValue(x))
//
//  implicit val utcTimeTypeMapper: TypeMapper[PbUTCTime, UTCTime] = TypeMapper[PbUTCTime, UTCTime](
//    x ⇒ UTCTime(x.getValue)
//  )(
//    x ⇒ PbUTCTime().withValue(x.value)
//  )
//
//  implicit val taiTimeTypeMapper: TypeMapper[PbTAITime, TAITime] = TypeMapper[PbTAITime, TAITime](
//    x ⇒ TAITime(x.getValue)
//  )(
//    x ⇒ PbTAITime().withValue(x.value)
//  )
//
//  implicit val keyTypeTypeMapper: TypeMapper[PbKeyType, KeyType[_]] =
//    TypeMapper[PbKeyType, KeyType[_]](x ⇒ KeyType.withName(x.toString()))(x ⇒ PbKeyType.fromName(x.toString).get)
//
//  implicit def arrayDataTypeMapper[T: ClassTag]: TypeMapper[PbArray, ArrayData[T]] =
//    TypeMapper[PbArray, ArrayData[T]](
//      x ⇒ ArrayData(x.values.map(decodePrimitive).toArray[T])
//    )(
//      x ⇒ PbArray().withValues(x.data.map(encodePrimitive))
//    )
//
//  implicit val obsIdTypeMapper: TypeMapper[String, Option[ObsId]] = TypeMapper[String, Option[ObsId]] { x ⇒
//    if (x.isEmpty) None else Some(ObsId(x))
//  } { x ⇒
//    x.getOrElse(empty).obsId
//  }
//
//  def encodeParamValue[P <: PbParamValue, T: ClassTag](paramValue: T)(implicit typeMapper: TypeMapper[P, T]): PbParamValue = {
//    def encodeArray(xs: mutable.WrappedArray[T]) = PbArray(xs.map(x => encodePrimitive[P, T](x).asInstanceOf[PbPrimitive]))
//
//    paramValue match {
//      case ArrayData(xs)   => PbParamValue(PbParamValue.Value.Array(encodeArray(xs)))
//      case MatrixData(xss) => PbParamValue(PbParamValue.Value.Matrix(PbMatrix(xss.map(encodeArray))))
//      case primitive: T    => PbParamValue().withPrimitive(encodePrimitive[P, T](primitive).asInstanceOf[PbPrimitive])
//    }
//  }
//
//  def encodePrimitive[P <: PbPrimitive, T](primitive: T)(implicit typeMapper: TypeMapper[P, T]): P = typeMapper.toBase(primitive)
//
//  def decodeParamValue[T](pbParamValue: PbParamValue): Any = pbParamValue.value match {
//    case x: Primitive                   => decodePrimitive(x)
//    case xs: PbParamValue.Value.Array   => xs.array.get.values.map(decodePrimitive)
//    case xss: PbParamValue.Value.Matrix => xss.matrix.get.values.map(arr => arr.values.map(decodePrimitive))
//  }
//
//  def decodePrimitive[T](primitive: Primitive): Any = primitive.value match {
//    case x: Option[Any] => x.get
//    case x              => x
//  }
//}
