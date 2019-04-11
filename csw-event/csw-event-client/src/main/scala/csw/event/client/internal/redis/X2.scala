package csw.event.client.internal.redis

import java.io.ByteArrayOutputStream

import csw.event.client.internal.redis.EventRomaineCodecs.EventRomaineCodec
import csw.params.core.formats.JsonSupport
import csw.params.core.generics.Key
import csw.params.core.generics.KeyType.StringKey
import csw.params.core.models.{Id, Prefix}
import csw.params.events.{EventName, SystemEvent}
import csw.time.core.models.UTCTime
import ujson.play.PlayJson

object X2 extends App {

  val payloadKey: Key[String] = StringKey.make("payloadKey")
  val payload                 = ("0" * 10000000)

  val event = SystemEvent(Prefix("x.y"), EventName("dummy")).copy(
    eventId = Id(1.toString),
    paramSet = Set(payloadKey.set(payload)),
    eventTime = UTCTime.now()
  )

  private val pb = {
    val begin  = System.currentTimeMillis()
    val length = EventRomaineCodec.toBytes(event).array().length
    println(System.currentTimeMillis() - begin)
    println(length)
  }

  println("***********************")

  val json = {
    val begin   = System.currentTimeMillis()
    val jsValue = JsonSupport.writeEvent(event)
    val length  = jsValue.toString().getBytes.length
    println(System.currentTimeMillis() - begin)
    println(length)
  }

  println("***********************")

  val msgpk = {
    val begin          = System.currentTimeMillis()
    val wr             = new upack.MsgPackWriter(new ByteArrayOutputStream())
    val jsValue        = JsonSupport.writeEvent(event)
    val x: Array[Byte] = PlayJson.transform(jsValue, wr).toByteArray

    val length = x.length
    println(System.currentTimeMillis() - begin)
    println(length)
  }

}
