package csw.event.client.pb

import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import csw.params.core.generics.KeyType
import csw.params.core.generics.KeyType.IntMatrixKey
import csw.params.core.models._
import csw.params.javadsl.JKeyType
import csw.time.core.models.{TAITime, UTCTime}

import csw_protobuf.parameter._
import org.scalatest.{FunSuite, Matchers}

import scala.collection.mutable

// DEOPSCSW-297: Merge protobuf branch in master
class PbParameterTest extends FunSuite with Matchers {

  test("should able to parse PbParameter with multiple values") {
    val parameter: PbParameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.centimeter)
      .addValues(PbInt(1), PbInt(2))

    val pbParam: PbParameter       = PbParameter.parseFrom(parameter.toByteArray)
    val parsedPbParam: PbParameter = PbParameter.parseFrom(pbParam.toByteString.toByteArray)

    pbParam shouldBe parsedPbParam
  }

  test("should able to parse PbParameter with sequence of values") {
    val parameter: PbParameter = PbParameter()
      .withName("encoder")
      .addValues(PbInt(1), PbInt(2), PbInt(3), PbInt(4))

    val parameter1: PbParameter = PbParameter.parseFrom(parameter.toByteArray)
    val parsedParameter         = PbParameter.parseFrom(parameter1.toByteString.toByteArray)

    parameter shouldEqual parsedParameter
  }

  test("should able to create Boolean Items") {
    val booleanItems       = PbArray().addValues(PbBoolean(true), PbBoolean(false))
    val parsedBooleanItems = PbArray.parseFrom(booleanItems.toByteString.toByteArray)
    booleanItems shouldBe parsedBooleanItems
  }

  // DEOPSCSW-661: Create UTCTimeKey and TAITimeKey replacing TimestampKey in Protobuf parameters
  test("should able to create PbParameter with UTCTime items") {
    val now       = UTCTime.now()
    val pbUTCTime = PbUTCTime().withValue(Timestamp.of(now.value.getEpochSecond, now.value.getNano))
    val parameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.second)
      .withKeyType(KeyType.UTCTimeKey)
      .addValues(pbUTCTime)

    parameter.name shouldBe "encoder"
    parameter.units shouldBe Units.second
    parameter.keyType shouldBe KeyType.UTCTimeKey
    parameter.values shouldBe List(pbUTCTime)
  }

  // DEOPSCSW-661: Create UTCTimeKey and TAITimeKey replacing TimestampKey in Protobuf parameters
  test("should able to create PbParameter with TAITime items") {
    val now       = TAITime.now()
    val pbTAITime = PbTAITime().withValue(Timestamp.of(now.value.getEpochSecond, now.value.getNano))

    val parameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.second)
      .withKeyType(KeyType.TAITimeKey)
      .addValues(pbTAITime)

    parameter.name shouldBe "encoder"
    parameter.units shouldBe Units.second
    parameter.keyType shouldBe KeyType.TAITimeKey
    parameter.values shouldBe List(pbTAITime)
  }

  test("should able to create PbParameter with Byte items") {
    val parameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.second)
      .withKeyType(KeyType.UTCTimeKey)
      .addValues(
        PbBytes().withValue(ByteString.copyFrom(Array[Byte](1, 2, 3, 4)))
      )

    parameter.name shouldBe "encoder"
    parameter.units shouldBe Units.second
    parameter.keyType shouldBe KeyType.UTCTimeKey
    parameter.values.head.asInstanceOf[PbBytes].value.toByteArray shouldBe Array[Byte](1, 2, 3, 4)
  }

  test("should able to create PbParameter with Choice items") {
    val choices = Seq(PbChoice("a345"), PbChoice("b234"), PbChoice("c567"), PbChoice("d890"))

    val parameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.second)
      .withKeyType(KeyType.UTCTimeKey)
      .withValues(choices)

    parameter.name shouldBe "encoder"
    parameter.units shouldBe Units.second
    parameter.keyType shouldBe KeyType.UTCTimeKey
  }

  test("should able to create PbParameter with int items only when KeyType is Int") {
    val parameter = PbParameter()
      .withName("encoder")
      .withUnits(Units.second)
      .withKeyType(KeyType.IntKey)
      .addValues(PbInt(1))

    parameter.name shouldBe "encoder"
    parameter.units shouldBe Units.second
    parameter.keyType shouldBe KeyType.IntKey
    parameter.values.map(TypeMapperSupport.paramValueMapper.toCustom) shouldBe List(1)
  }

  test("should able to create PbParameter and compare with Parameter for Int and String Key") {
    val key   = KeyType.IntKey.make("encoder")
    val param = key.set(1, 2, 3, 4)

    val pbParam = PbParameter()
      .withName("encoder")
      .withUnits(Units.angstrom)
      .addValues(PbInt(1), PbInt(2), PbInt(3), PbInt(4))

    val items = pbParam.values.map(TypeMapperSupport.paramValueMapper.toCustom)
    items shouldBe Seq(1, 2, 3, 4)

    val parsedPbParam = PbParameter.parseFrom(pbParam.toByteArray)

    pbParam shouldBe parsedPbParam
    parsedPbParam should not be param

    val key2   = KeyType.StringKey.make("encoder")
    val param2 = key2.set("abc", "xyc")

    val pbParam2 = PbParameter()
      .withName("encoder")
      .addValues(PbString("abc"), PbString("xyz"))
    val parsedPbParam2 = PbParameter.parseFrom(pbParam2.toByteArray)

    pbParam2 shouldEqual parsedPbParam2
    parsedPbParam2 should not be param2
  }

  test("should able to create PbParameter with ArrayItems") {
    val array1 = PbArray().addValues(PbInt(1000), PbInt(2000), PbInt(3000))
    val array2 = PbArray().addValues(PbInt(-1), PbInt(-2), PbInt(-3))
    val parameter: PbParameter = PbParameter()
      .withName("encoder")
      .addValues(array1, array2)

    val values = parameter.values.map(TypeMapperSupport.paramValueMapper.toCustom)
    values.head shouldBe ArrayData(Seq(1000, 2000, 3000).to[mutable.WrappedArray])
    values.tail.head shouldBe ArrayData(Seq(-1, -2, -3).to[mutable.WrappedArray])
  }

  test("should able to create PbParameter with MatrixItems") {
    val matrix = PbMatrix().addValues(
      PbArray().addValues(PbInt(1), PbInt(2), PbInt(3)),
      PbArray().addValues(PbInt(4), PbInt(5), PbInt(6))
    )

    val parameter: PbParameter = PbParameter()
      .withName("encoder")
      .addValues(matrix)

    TypeMapperSupport.paramMapper.toCustom(parameter).items.head shouldBe MatrixData(
      Seq(
        Seq(1, 2, 3).to[mutable.WrappedArray],
        Seq(4, 5, 6).to[mutable.WrappedArray]
      ).to[mutable.WrappedArray]
    )
  }

  test("should able to change the type from/to PbParameter to/from Parameter for IntKey") {
    val key         = KeyType.IntKey.make("encoder")
    val param       = key.set(1, 2, 3, 4)
    val mapper      = TypeMapperSupport.paramMapper
    val mappedParam = mapper.toCustom(mapper.toBase(param))

    param shouldEqual mappedParam
  }

  // DEOPSCSW-661: Create UTCTimeKey and TAITimeKey replacing TimestampKey in Protobuf parameters
  test("should able to change the type from/to PbParameter to/from Parameter for UTCTimeKey") {
    val mapper      = TypeMapperSupport.paramMapper
    val key         = KeyType.UTCTimeKey.make("utcTimeKey")
    val param       = key.set(UTCTime.now())
    val mappedParam = mapper.toCustom(mapper.toBase(param))

    param shouldEqual mappedParam
  }

  // DEOPSCSW-661: Create UTCTimeKey and TAITimeKey replacing TimestampKey in Protobuf parameters
  test("should able to change the type from/to PbParameter to/from Parameter for TAITimeKey") {
    val mapper      = TypeMapperSupport.paramMapper
    val key         = KeyType.TAITimeKey.make("taiTimeKey")
    val param       = key.set(TAITime.now())
    val mappedParam = mapper.toCustom(mapper.toBase(param))

    param shouldEqual mappedParam
  }

  test("should able to change the type from/to PbParameter to/from Parameter for IntArrayKey") {
    val mapper      = TypeMapperSupport.paramMapper
    val key         = KeyType.IntArrayKey.make("blah")
    val param       = key.set(ArrayData.fromArray(1, 2, 3))
    val mappedParam = mapper.toCustom(mapper.toBase(param))

    param shouldEqual mappedParam
  }

  test("should able to change the type from/to PbParameter to/from Parameter for IntMatrixKey") {
    val mapper      = TypeMapperSupport.paramMapper
    val key         = KeyType.IntMatrixKey.make("blah")
    val param       = key.set(MatrixData.fromArrays(Array(1, 2, 3), Array(6, 7)))
    val mappedParam = mapper.toCustom(mapper.toBase(param))

    param shouldEqual mappedParam
    mappedParam.keyType shouldBe IntMatrixKey
  }

  test("should able to change the type from/to PbParameter to/from Parameter using java api") {
    val key         = JKeyType.IntKey.make("encoder")
    val param       = key.set(1, 2, 3, 4)
    val mapper      = TypeMapperSupport.paramMapper
    val parsedParam = mapper.toCustom(mapper.toBase(param))

    parsedParam shouldEqual param
  }

  test("should able to change the type from/to PbParameter to/from Parameter for RaDec") {
    PbRaDec()
    PbRaDec.defaultInstance

    PbRaDec().withRa(10).withDec(32)
    PbRaDec().withRa(10).withDec(321)

    val raDec = RaDec(1, 2)
    val param = KeyType.RaDecKey.make("Asd").set(raDec)

    val pbParameter = TypeMapperSupport.paramMapper.toBase(param)
    pbParameter.toByteArray

    param.keyName shouldBe pbParameter.name
    param.units shouldBe pbParameter.units
    param.keyType shouldBe pbParameter.keyType
    param.items shouldBe pbParameter.values.map(TypeMapperSupport.paramValueMapper.toCustom)
  }
}
