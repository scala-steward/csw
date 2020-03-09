package csw.benchmark.event

import java.time.Instant

import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models._
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.time.core.models.TAITime

object Data {
  private val byteKey       = KeyType.ByteKey.make("bytes")
  private val intKey        = KeyType.IntKey.make("ints")
  private val doubleKey     = KeyType.DoubleKey.make("doubles")
  private val floatKey      = KeyType.FloatKey.make("floats")
  private val stringKey     = KeyType.StringKey.make("strings")
  private val radecKey      = KeyType.RaDecKey.make("radecs")
  private val choiceKey     = KeyType.ChoiceKey.make("choices", Choices(Set(Choice("100"))))
  private val taiTimeKey    = KeyType.TAITimeKey.make("tai-times")
  private val arrayDataKey  = KeyType.IntArrayKey.make("intarrays")
  private val matrixDataKey = KeyType.IntMatrixKey.make("intmatrices")
  private val structKey     = KeyType.StructKey.make("structs")

  private val paramSet: Set[Parameter[_]] = Set(
    byteKey.set(100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100),
    intKey.set(100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100),
    doubleKey.set(100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342,
      100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342),
    floatKey.set(100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f,
      100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f),
    stringKey.set(
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100"
    ),
    radecKey.set(
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100)
    )
  )
  private val baseEvent       = SystemEvent(Prefix("a.b"), EventName("eventName1"))
  val data: Parameter[Struct] = structKey.setAll((1 to 150).map(_ => Struct(paramSet)).toArray)
  val bigEvent: SystemEvent   = baseEvent.copy(paramSet = Set(data))

  val smallEvent: SystemEvent = baseEvent.copy(
    paramSet = Set(
      intKey.set(1, 2, 3),
      radecKey.set(RaDec(100, 100)),
      choiceKey.set(Choice("100")),
      taiTimeKey.set(TAITime(Instant.ofEpochSecond(20, 20))),
      arrayDataKey.set(ArrayData(Array(10, 20, 30))),
      matrixDataKey.set(MatrixData.fromArrays(Array(Array(10, 20, 30)))),
      structKey.set(Struct(Set(KeyType.IntKey.make("ints").set(4, 5, 6))))
    )
  )
}
