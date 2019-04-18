package csw.event.client.pb
import java.time.Instant

import com.google.protobuf.timestamp.Timestamp
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.{ArrayData, Choice, MatrixData, RaDec}
import csw.time.core.models.{TAITime, UTCTime}
import csw_protobuf2.parameter2.PbParamValue2.Data.Value
import csw_protobuf2.parameter2._
import play.api.libs.json.Format
import scalapb.TypeMapper

import scala.collection.mutable
import scala.reflect.ClassTag

object TypeMapperSupport2 {

  implicit val raDecTypeMapper2: TypeMapper[PbRaDec2, RaDec] =
    TypeMapper[PbRaDec2, RaDec](x ⇒ RaDec(x.ra, x.dec))(x ⇒ PbRaDec2().withRa(x.ra).withDec(x.dec))

  implicit val instantMapper2: TypeMapper[Timestamp, Instant] =
    TypeMapper[Timestamp, Instant] { x =>
      Instant.ofEpochSecond(x.seconds, x.nanos)
    } { x =>
      Timestamp().withSeconds(x.getEpochSecond).withNanos(x.getNano)
    }

  implicit val utcTimeTypeMapper2: TypeMapper[PbUTCTime2, UTCTime] = TypeMapper[PbUTCTime2, UTCTime](
    x ⇒ UTCTime(instantMapper2.toCustom(x.getValue))
  )(
    x ⇒ PbUTCTime2().withValue(instantMapper2.toBase(x.value))
  )

  implicit val taiTimeTypeMapper2: TypeMapper[PbTAITime2, TAITime] = TypeMapper[PbTAITime2, TAITime](
    x ⇒ TAITime(instantMapper2.toCustom(x.getValue))
  )(
    x ⇒ PbTAITime2().withValue(instantMapper2.toBase(x.value))
  )

  implicit val choiceTypeMapper2: TypeMapper[String, Choice] = TypeMapper[String, Choice](Choice.apply)(_.name)

  implicit val charTypeMapper2: TypeMapper[PbChar2, Char] =
    TypeMapper[PbChar2, Char](p => p.string.head)(x => PbChar2(x.toString))

  implicit def keyTypeMapper2[S: Format: ClassTag]: TypeMapper[String, KeyType[S]] =
    //incorrect
    TypeMapper[String, KeyType[S]](
      s => KeyType.withNameInsensitive(s).asInstanceOf[KeyType[S]]
    )(x => x.entryName)

//  implicit def aaa[S: TypeMapper[PbPrimitive2,S]] : TypeMapper[PbPrimitive2,S] = implicitly[TypeMapper[PbPrimitive2,S]]

  def pbArrayTypeMapper[S: Format: ClassTag: TypeMapper[PbPrimitive2, S]]: TypeMapper[PbArray2, mutable.WrappedArray[S]] =
    TypeMapper[PbArray2, mutable.WrappedArray[S]](
      x => new mutable.WrappedArray.ofRef[S](x.elements.toArray[S])
    )(y => {
      val value: TypeMapper[PbPrimitive2, S] = implicitly[TypeMapper[PbPrimitive2, S]]
      PbArray2(y.map(value.toBase))
    })

//  implicit def paramValueMapper2[S: Format: ClassTag]: TypeMapper[PbParamValue2, mutable.WrappedArray[S]] =
//    TypeMapper[PbParamValue2, mutable.WrappedArray[S]] {
////      case PbPrimitive2(data)  => new mutable.WrappedArray.ofRef[S](Array(data.asInstanceOf[S]))
//      case PbParamValue2(data)  => if(data.isArray) new mutable.WrappedArray.ofRef[S](pbArrayTypeMapper())
////      case PbMatrixData2(data) => new mutable.WrappedArray.ofRef[S](data.asInstanceOf[Array[S]])
//    }(x => x.entryName)

//  implicit def parameterTypeMapper2[S: Format: ClassTag]: TypeMapper[PbParameter2, Parameter[S]] =
//    TypeMapper[PbParameter2, Parameter[S]](
//      pbParameter =>
//        Parameter(
//          pbParameter.key,
//          keyTypeMapper2[S].toCustom(pbParameter.key),
//          cswItems(pbParameter.value),
//          pbParameter.units
//      )
//    )(parameter =>
//    PbParameter2(parameter.keyType.entryName, )
//    )

  {
    val out = PbParameter2()
      .withKeyName("abc")
      .withUnits("dd")
      .withItems(
        Seq(
          PbParamValue2()
            .withPrimitive(
              PbPrimitive2().withInt(100)
            )
        )
      )

    out.keyName
    out.units
    out.items.head.getMatrix.arrays
  }
  def decode(pbParameter2: PbParameter2): Parameter[_] =
    Parameter(
      pbParameter2.keyName,
      KeyType.withNameInsensitive(pbParameter2.keyType),
      pbParameter2.items
    )

  def encode(parameter: Parameter[_]): PbParameter2 =
    PbParameter2()
      .withKeyName(parameter.keyName)
      .withKeyType(parameter.keyType.entryName)
      .withItems(parameter.items.map(put))
      .withUnits(parameter.units.entryName)

  def put(item: Any): PbParamValue2 = item match {
    case _: Int | Double | RaDec => PbParamValue2().withPrimitive(putPrimitive(item))
    case ArrayData(xs)           => PbParamValue2().withArray(PbArrayData2().withElements(xs.map(putPrimitive)))
    case MatrixData(xss) =>
      PbParamValue2().withMatrix(
        PbMatrixData2().withArrays(
          xss.map { xs =>
            PbArrayData2().withElements(xs.map(putPrimitive))
          }
        )
      )
  }

  def putPrimitive(item: Any): PbPrimitive2 = item match {
    case x: Int         => PbPrimitive2().withInt(x)
    case x: Double      => PbPrimitive2().withDouble(x)
    case RaDec(ra, dec) => PbPrimitive2().withPbradec(PbRaDec2().withRa(ra).withDec(dec))
  }

  def get(item: PbParamValue2): Any = ??? //item match {
//    case PbPrimitive2(data) => getPrimitive(data) PbParamValue2 ().withPrimitive(putPrimitive(item))
//    case ArrayData(xs)      => PbParamValue2().withArray(PbArrayData2().withElements(xs.map(putPrimitive)))
//    case MatrixData(xss) =>
//      PbParamValue2().withMatrix(
//        PbMatrixData2().withArrays(
//          xss.map { xs =>
//            PbArrayData2().withElements(xs.map(putPrimitive))
//          }
//        )
//      )
  //}

  def getPrimitive(item: PbPrimitive2): Any = item.data match {
    case PbPrimitive2.Data.Int(v)                     => v
    case PbPrimitive2.Data.Double(v)                  => v
    case PbPrimitive2.Data.Pbradec(PbRaDec2(ra, dec)) => RaDec(ra, dec)
  }

}
