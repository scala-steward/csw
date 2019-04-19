//package csw.event.client.pb
//
//import csw.params.core.formats.MiscJsonFormats
//import csw.params.core.generics.{KeyType, Parameter}
//import csw.params.core.models._
//import csw.time.core.models.{TAITime, UTCTime}
//import csw_protobuf.parameter._
//import csw_protobuf.primitives._
//import play.api.libs.json.Format
//import scalapb.TypeMapper
//import TypeMapperSupport._
//
//import scala.reflect.ClassTag
//
//object TypeMapperFactory extends MiscJsonFormats {
//
//  def make(keyType: KeyType[_]): TypeMapper[PbParameter, Parameter[_]] =
//    keyType match {
//      case KeyType.ChoiceKey       ⇒ typeMapper[PbChoice, Choice]
//      case KeyType.RaDecKey        ⇒ typeMapper[PbRaDec, RaDec]
//      case KeyType.StringKey       ⇒ typeMapper[PbString, String]
//      case KeyType.StructKey       ⇒ typeMapper[PbStruct, Struct]
//      case KeyType.UTCTimeKey      ⇒ typeMapper[PbUTCTime, UTCTime]
//      case KeyType.TAITimeKey      ⇒ typeMapper[PbTAITime, TAITime]
//      case KeyType.BooleanKey      ⇒ typeMapper[PbBoolean, Boolean]
//      case KeyType.CharKey         ⇒ typeMapper[PbChar, Char]
//      case KeyType.ByteKey         ⇒ typeMapper[PbBytes, Byte]
//      case KeyType.ShortKey        ⇒ typeMapper[PbShort, Short]
//      case KeyType.LongKey         ⇒ typeMapper[PbLong, Long]
//      case KeyType.IntKey          ⇒ typeMapper[PbInt, Int]
//      case KeyType.FloatKey        ⇒ typeMapper[PbFloat, Float]
//      case KeyType.DoubleKey       ⇒ typeMapper[PbDouble, Double]
//      case KeyType.ByteArrayKey    ⇒ typeMapper[PbArray, ArrayData[Byte]]
//      case KeyType.ShortArrayKey   ⇒ typeMapper[PbArray, ArrayData[Short]]
//      case KeyType.LongArrayKey    ⇒ typeMapper[PbArray, ArrayData[Long]]
//      case KeyType.IntArrayKey     ⇒ typeMapper[PbArray, ArrayData[Int]]
//      case KeyType.FloatArrayKey   ⇒ typeMapper[PbArray, ArrayData[Float]]
//      case KeyType.DoubleArrayKey  ⇒ typeMapper[PbArray, ArrayData[Double]]
//      case KeyType.ByteMatrixKey   ⇒ typeMapper[PbMatrix, MatrixData[Byte]]
//      case KeyType.ShortMatrixKey  ⇒ typeMapper[PbMatrix, MatrixData[Short]]
//      case KeyType.LongMatrixKey   ⇒ typeMapper[PbMatrix, MatrixData[Long]]
//      case KeyType.IntMatrixKey    ⇒ typeMapper[PbMatrix, MatrixData[Int]]
//      case KeyType.FloatMatrixKey  ⇒ typeMapper[PbMatrix, MatrixData[Float]]
//      case KeyType.DoubleMatrixKey ⇒ typeMapper[PbMatrix, MatrixData[Double]]
//      case _                       => throw new RuntimeException("Invalid key type")
//    }
//
//  private def typeMapper[P <: PbPrimitive, T: ClassTag: Format](
//      implicit typeMapper: TypeMapper[P, T]
//  ): TypeMapper[PbParameter, Parameter[_]] = {
//    TypeMapperSupport.parameterTypeMapper[P, T].asInstanceOf[TypeMapper[PbParameter, Parameter[_]]]
//  }
//
//}
