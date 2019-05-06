package csw.params.core.formats

import csw.params.core.formats.CborSupport._
import csw.params.core.generics.{Key, Parameter}
import csw.params.core.models._
import csw.params.events.{EventName, ObserveEvent, SystemEvent}
import csw.params.testdata.ParamSetData
import io.bullet.borer.Cbor
import org.scalatest.{FunSuite, Matchers}
import csw.params.javadsl.JKeyType

class CborTest extends FunSuite with Matchers {

  private val prefix: Prefix       = Prefix("wfos.blue.filter")
  private val eventName: EventName = EventName("filter wheel")

  test("should able to serialize and deserialize observe event with paramSet having all key-types") {
    val event = ObserveEvent(prefix, eventName, ParamSetData.paramSet)

    val bytes       = Cbor.encode(event).toByteArray
    val parsedEvent = Cbor.decode(bytes).to[ObserveEvent].value

    parsedEvent shouldEqual event
  }

  test("should able to serialize and deserialize system event with paramSet having all key-types") {
    val event = SystemEvent(prefix, eventName, ParamSetData.paramSet)

    val bytes       = Cbor.encode(event).toByteArray
    val parsedEvent = Cbor.decode(bytes).to[SystemEvent].value

    parsedEvent shouldEqual event
  }

  test("byte key interop from java to scala") {
    val jByteKey: Key[java.lang.Byte]    = JKeyType.ByteKey.make("bytes")
    val param: Parameter[java.lang.Byte] = jByteKey.set("abc".getBytes().map(x ⇒ x: java.lang.Byte))
    val bytes: Array[Byte]               = Cbor.encode(param).toByteArray

    val parsedParam = Cbor.decode(bytes).to[Parameter[Byte]].value
    parsedParam shouldEqual param
  }

  test("int key interop from java to scala") {
    val jIntKey: Key[Integer]     = JKeyType.IntKey.make("ints")
    val param: Parameter[Integer] = jIntKey.set(Array(1, 2, 3).map(x ⇒ Integer.valueOf(x)))
    val bytes: Array[Byte]        = Cbor.encode(param).toByteArray

    val parsedParam = Cbor.decode(bytes).to[Parameter[Int]].value
    parsedParam shouldEqual param
  }
}
