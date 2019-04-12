package csw.event.client.internal.redis

import java.io.ByteArrayOutputStream

import csw.event.client.internal.redis.EventRomaineCodecs.EventRomaineCodec
import csw.params.core.formats.JsonSupport
import csw.params.core.generics.KeyType.{DoubleKey, FloatKey, IntKey}
import csw.params.core.generics.{Key, KeyType}
import csw.params.core.models.{Id, Prefix}
import csw.params.events.{EventName, SystemEvent}
import csw.time.core.models.UTCTime
import ujson.play.PlayJson

object X2 extends App {

//  val payloadKey: Key[String] = StringKey.make("payloadKey")
//  val payload                 = "0" * 10000000

//  val payloadKey: Key[Byte] = ByteKey.make("payloadKey")
//  val payload               = ("0" * 10000000).getBytes

//  val payloadKey: Key[Long] = LongKey.make("payloadKey")
//  val payload: Long         = Long.MaxValue

  val payloadKey: Key[Double] = DoubleKey.make("payloadKey")
  val payload: Double         = Double.MaxValue

  val event = SystemEvent(Prefix("x.y"), EventName("dummy")).copy(
    eventId = Id(1.toString),
    paramSet = Set(payloadKey.set(payload)),
    eventTime = UTCTime.now()
  )

  println("************ PB ***********")

  private val pb = {
    val begin = System.currentTimeMillis()
    val array = EventRomaineCodec.toBytes(event).array
    println(System.currentTimeMillis() - begin)
    println(array.length)
  }

  println("*********** JSON ************")

  val json = {
    val begin = System.currentTimeMillis()
    val bytes = JsonSupport.writeEvent(event).toString().getBytes
    println(System.currentTimeMillis() - begin)
    println(bytes.length)
  }

  println("********** MsgPk *************")

  val msgpk = {
    val begin = System.currentTimeMillis()
    val x: Array[Byte] = PlayJson
      .transform(
        JsonSupport.writeEvent(event),
        new upack.MsgPackWriter(new ByteArrayOutputStream())
      )
      .toByteArray

    println(System.currentTimeMillis() - begin)
    println(x.length)
  }

}
