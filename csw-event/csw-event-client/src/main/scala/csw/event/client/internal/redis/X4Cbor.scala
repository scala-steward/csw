package csw.event.client.internal.redis
import csw.event.client.internal.redis.X2.event
import io.bullet.borer.Cbor

object X4Cbor {

  println("*********** CBOR ************")

  import csw.params.core.formats.CborFormats._

  val cbor = {
    val begin = System.currentTimeMillis()
    val bytes = Cbor.encode(event).toByteArray
    println(System.currentTimeMillis() - begin)
    println(bytes.length)
  }
}
