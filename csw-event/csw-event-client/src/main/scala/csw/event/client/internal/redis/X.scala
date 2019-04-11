package csw.event.client.internal.redis
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import csw.event.client.internal.redis.EventRomaineCodecs.EventRomaineCodec
import csw.params.core.models.Prefix
import csw.params.events.{EventName, SystemEvent}
import io.bullet.borer.Cbor
import io.bullet.borer.Dom.Element
import play.api.libs.json._
import ujson.BytesRenderer
import ujson.play.PlayJson

object X extends App {

  val e                         = SystemEvent(Prefix("x.y"), EventName("dummy"))
  private val bytes: ByteBuffer = EventRomaineCodec.toBytes(e)
  println(bytes)

  println("event=>" + EventRomaineCodec.fromBytes(bytes))

  private val playJson: JsObject = JsObject(
    Map(
      "aaaaaaa"  -> JsString("a" * 10000),
      "aaaaaaa1" -> JsArray((1 to 10).toList.map(_ => JsString("a" * 10000))),
      "aaaaaaa2" -> JsArray((1 to 10).toList.map(_ => JsNumber(10000))),
      "aaaaaaa3" -> JsArray((1 to 10).toList.map(_ => JsString("a" * 10000))),
      "aaaaaaa4" -> JsArray((1 to 10).toList.map(_ => JsString("a" * 10000))),
      "aaaaaaa5" -> JsArray((1 to 10).toList.map(_ => JsString("a" * 10000))),
      "aaaaaaa6" -> JsArray((1 to 10).toList.map(_ => JsString("a" * 10000))),
      "aaaaaaa7" -> JsArray((1 to 10).toList.map(_ => JsString("a" * 10000))),
      "aaaaaaa8" -> JsArray((1 to 10).toList.map(_ => JsString("a" * 10000))),
    )
  )

  private val jsObject: JsObject = Json.obj(
    "aaaaaaa"  -> "a" * 10000,
    "aaaaaaa1" -> (1 to 10).toList.map(_ => "a" * 10000),
    "aaaaaaa2" -> (1 to 10).toList.map(_ => 10000),
    "aaaaaaa3" -> (1 to 10).toList.map(_ => "a" * 10000),
    "aaaaaaa4" -> (1 to 10).toList.map(_ => "a" * 10000),
    "aaaaaaa5" -> (1 to 10).toList.map(_ => "a" * 10000),
    "aaaaaaa6" -> (1 to 10).toList.map(_ => "a" * 10000),
    "aaaaaaa7" -> (1 to 10).toList.map(_ => "a" * 10000),
    "aaaaaaa8" -> (1 to 10).toList.map(_ => "a" * 10000),
  )

  val dom = Element.Map(
    "aaaaaaa"  -> Element.Array((1 to 10).toList.map(_ => Element.Value.String("a" * 10000)): _*),
    "aaaaaaa1" -> Element.Array((1 to 10).toList.map(_ => Element.Value.Int(10000)): _*),
    "aaaaaaa2" -> Element.Array((1 to 10).toList.map(_ => Element.Value.String("a" * 10000)): _*),
    "aaaaaaa3" -> Element.Array((1 to 10).toList.map(_ => Element.Value.String("a" * 10000)): _*),
    "aaaaaaa4" -> Element.Array((1 to 10).toList.map(_ => Element.Value.String("a" * 10000)): _*),
    "aaaaaaa5" -> Element.Array((1 to 10).toList.map(_ => Element.Value.String("a" * 10000)): _*),
    "aaaaaaa6" -> Element.Array((1 to 10).toList.map(_ => Element.Value.String("a" * 10000)): _*),
    "aaaaaaa7" -> Element.Array((1 to 10).toList.map(_ => Element.Value.String("a" * 10000)): _*),
    "aaaaaaa8" -> Element.Array((1 to 10).toList.map(_ => Element.Value.String("a" * 10000)): _*),
  )

  private val bytes1: Array[Byte] = PlayJson.transform(jsObject, BytesRenderer()).toBytes

  private val wr                 = new upack.MsgPackWriter(new ByteArrayOutputStream())
  private val msgpk: Array[Byte] = PlayJson.transform(jsObject, wr).toByteArray

  val cbor = Cbor.encode(dom).toByteArray

  println(playJson.toString().getBytes.toList.length)
  println(jsObject.toString().getBytes.toList.length)
  println(bytes1.toList.length)
  println(msgpk.toList.length)
  println(cbor.length)
}
