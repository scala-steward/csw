package csw.params.core.formats

import csw.params.commands._
import csw.params.core.formats.CborSupport._
import csw.params.core.models._
import csw.params.events.{Event, EventName, ObserveEvent, SystemEvent}
import csw.params.testdata.ParamSetData
import io.bullet.borer.Cbor
import org.scalatest.{FunSuite, Matchers}

class CborTest extends FunSuite with Matchers {

  private val prefix      = Prefix("wfos.blue.filter")
  private val eventName   = EventName("filter wheel")
  private val commandName = CommandName("filter wheel")
  private val maybeObsId  = Some(ObsId("obsId"))

  test("should encode and decode event with paramSet having all key-types") {
    val event: Event = ObserveEvent(prefix, eventName, ParamSetData.paramSet)
    val bytes        = Cbor.encode(event).toByteArray
    val parsedEvent  = Cbor.decode(bytes).to[Event].value
    parsedEvent shouldEqual event
  }

  test("should encode base-type event and decode concrete-type") {
    val event: Event = ObserveEvent(prefix, eventName, ParamSetData.paramSet)
    val bytes        = Cbor.encode(event).toByteArray
    val parsedEvent  = Cbor.decode(bytes).to[ObserveEvent].value
    parsedEvent shouldEqual event
  }

  test("should encode and decode observe event") {
    val observeEvent = ObserveEvent(prefix, eventName)
    val bytes        = Cbor.encode(observeEvent).toByteArray
    val parsedEvent  = Cbor.decode(bytes).to[ObserveEvent].value
    parsedEvent shouldEqual observeEvent
  }

  test("should encode and decode system event") {
    val systemEvent = SystemEvent(prefix, eventName)
    val bytes       = Cbor.encode(systemEvent).toByteArray
    val parsedEvent = Cbor.decode(bytes).to[SystemEvent].value
    parsedEvent shouldEqual systemEvent
  }

  test("should encode and decode a command with paramSet having all key-types") {
    val command: Command = Setup(prefix, commandName, maybeObsId, ParamSetData.paramSet)
    val bytes            = Cbor.encode(command).toByteArray
    val parsedCommand    = Cbor.decode(bytes).to[Command].value
    parsedCommand shouldEqual command
  }

  //  test("should encode base-type command and decode a concrete-type") {
  //    val command: Command = Setup(prefix, commandName, maybeObsId)
  //    val bytes            = Cbor.encode(command).toByteArray
  //    val parsedCommand    = Cbor.decode(bytes).to[Setup].value
  //    parsedCommand shouldEqual command
  //  }

  test("should encode and decode a setup command") {
    val setupCommand: Setup = Setup(prefix, commandName, maybeObsId)
    val bytes               = Cbor.encode(setupCommand).toByteArray
    val parsedCommand       = Cbor.decode(bytes).to[Setup].value
    parsedCommand shouldEqual setupCommand
  }

  test("should encode and decode an observe command") {
    val observeCommand: Observe = Observe(prefix, commandName, maybeObsId)
    val bytes                   = Cbor.encode(observeCommand).toByteArray
    val parsedCommand           = Cbor.decode(bytes).to[Observe].value
    parsedCommand shouldEqual observeCommand
  }

  test("should encode and decode a wait command") {
    val waitCommand: Wait = Wait(prefix, commandName, maybeObsId)
    val bytes             = Cbor.encode(waitCommand).toByteArray
    val parsedCommand     = Cbor.decode(bytes).to[Wait].value
    parsedCommand shouldEqual waitCommand
  }
}
