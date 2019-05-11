package csw.event.client.internal.redis

import java.nio.ByteBuffer

import csw.params.events.{Event, EventKey}
import csw.event.client.pb.PbConverter
import csw_protobuf.events.PbEvent
import io.bullet.borer.Cbor
import romaine.codec.{RomaineByteCodec, RomaineStringCodec}

import scala.util.control.NonFatal

/**
 * Encodes and decodes keys as EventKeys and values as ProtoBuf byte equivalent of Event
 */
private[event] object EventRomaineCodecs {
  import csw.params.core.formats.CborSupport._

  implicit object EventKeyRomaineCodec extends RomaineStringCodec[EventKey] {
    override def toString(eventKey: EventKey): String = eventKey.key
    override def fromString(str: String): EventKey    = EventKey(str)
  }

  implicit object EventRomaineCodec extends RomaineByteCodec[Event] {
    override def toBytes(event: Event): ByteBuffer = {
//      val pbEvent = PbConverter.toPbEvent(event)
//      ByteBuffer.wrap(pbEvent.toByteArray)
      ByteBuffer.wrap(Cbor.encode(event).toByteArray)
    }
    override def fromBytes(byteBuffer: ByteBuffer): Event = {
      try {
        val bytes = new Array[Byte](byteBuffer.remaining)
        byteBuffer.get(bytes)
        Cbor.decode(bytes).to[Event].value
//        PbConverter.fromPbEvent(PbEvent.parseFrom(bytes))
      } catch {
        case NonFatal(_) ⇒ Event.badEvent()
      }
    }
  }

}
