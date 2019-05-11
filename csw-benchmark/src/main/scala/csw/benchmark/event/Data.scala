package csw.benchmark.event

import java.time.Instant

import csw.params.core.generics.{Key, KeyType, Parameter}
import csw.params.core.models.{ArrayData, Choice, Choices, MatrixData, Prefix, RaDec, Struct}
import csw.params.events.{EventName, SystemEvent}
import csw.time.core.models.TAITime

import scala.collection.mutable

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
  private val structKey: Key[Struct] = KeyType.StructKey.make("abc")
  val data: Parameter[Struct]        = structKey.set((1 to 150).map(_ => Struct(paramSet)): _*)
  val event: SystemEvent             = SystemEvent(Prefix("a.b"), EventName("eventName1")).copy(paramSet = Set(data))

  val smallEvent: SystemEvent = SystemEvent(Prefix("a.b"), EventName("eventName1")).copy(
    paramSet = Set(
      intKey.set(1, 2, 3),
      radecKey.set(RaDec(100, 100)),
      choiceKey.set(Choice("100")),
      taiTimeKey.set(TAITime(Instant.ofEpochSecond(20, 20))),
      arrayDataKey.set(ArrayData(Array(10, 20, 30))),
      matrixDataKey.set(MatrixData(Array(Array(10, 20, 30).to[mutable.WrappedArray]))),
      structKey.set(Struct(Set(KeyType.IntKey.make("ints").set(4, 5, 6))))
    )
  )
}
