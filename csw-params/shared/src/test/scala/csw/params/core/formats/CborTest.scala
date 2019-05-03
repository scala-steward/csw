package csw.params.core.formats

import csw.params.core.formats.CborSupport._
import csw.params.core.models._
import csw.params.events.{EventName, ObserveEvent}
import csw.params.testdata.ParamSetData
import io.bullet.borer.Cbor
import org.scalatest.{FunSuite, Matchers}

class CborTest extends FunSuite with Matchers {

  private val prefix: Prefix       = Prefix("wfos.blue.filter")
  private val eventName: EventName = EventName("filter wheel")

  test("should able to serialize and deserialize event with paramSet having all key-types") {
    val event = ObserveEvent(prefix, eventName, ParamSetData.paramSet)

    val bytes       = Cbor.encode(event).toByteArray
    val parsedEvent = Cbor.decode(bytes).to[ObserveEvent].value

    parsedEvent shouldEqual event
  }
}
