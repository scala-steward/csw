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

  test("should encode and decode observe event with paramSet having all key-types") {
    val observeEvent = ObserveEvent(prefix, eventName, ParamSetData.paramSet)
    val bytes        = Cbor.encode(observeEvent).toByteArray
    val parsedEvent  = Cbor.decode(bytes).to[ObserveEvent].value
    parsedEvent shouldEqual observeEvent
  }

  test("should encode and decode system event with paramSet having all key-types") {
    val systemEvent = SystemEvent(prefix, eventName, ParamSetData.paramSet)
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

  test("should encode and decode a sequence command with paramSet having all key-types") {
    val sequenceCommand: SequenceCommand = Wait(prefix, commandName, maybeObsId, ParamSetData.paramSet)
    val bytes                            = Cbor.encode(sequenceCommand).toByteArray
    val parsedCommand                    = Cbor.decode(bytes).to[SequenceCommand].value
    parsedCommand shouldEqual sequenceCommand
  }

  test("should encode and decode a control command with paramSet having all key-types") {
    val controlCommand: ControlCommand = Setup(prefix, commandName, maybeObsId, ParamSetData.paramSet)
    val bytes                          = Cbor.encode(controlCommand).toByteArray
    val parsedCommand                  = Cbor.decode(bytes).to[ControlCommand].value
    parsedCommand shouldEqual controlCommand
  }

  test("should encode and decode a setup command with paramSet having all key-types") {
    val setupCommand: Setup = Setup(prefix, commandName, maybeObsId, ParamSetData.paramSet)
    val bytes               = Cbor.encode(setupCommand).toByteArray
    val parsedCommand       = Cbor.decode(bytes).to[Setup].value
    parsedCommand shouldEqual setupCommand
  }

  test("should encode and decode an observe command with paramSet having all key-types") {
    val observeCommand: Observe = Observe(prefix, commandName, maybeObsId, ParamSetData.paramSet)
    val bytes                   = Cbor.encode(observeCommand).toByteArray
    val parsedCommand           = Cbor.decode(bytes).to[Observe].value
    parsedCommand shouldEqual observeCommand
  }

  test("should encode and decode a wait command with paramSet having all key-types") {
    val waitCommand: Wait = Wait(prefix, commandName, maybeObsId, ParamSetData.paramSet)
    val bytes             = Cbor.encode(waitCommand).toByteArray
    val parsedCommand     = Cbor.decode(bytes).to[Wait].value
    parsedCommand shouldEqual waitCommand
  }
}
