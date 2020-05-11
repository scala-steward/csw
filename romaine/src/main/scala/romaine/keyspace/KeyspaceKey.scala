package romaine.keyspace

import romaine.codec.RomaineCodec

case class KeyspaceKey(id: KeyspaceId, value: String) {
  override def toString: String = s"${id.entryName}:$value"
}

object KeyspaceKey {
  def parse(string: String): KeyspaceKey =
    string.split(":") match {
      case Array(pre, post) => KeyspaceKey(KeyspaceId.withName(pre), post)
      case _                => throw new RuntimeException(s"error in parsing keyspace-key=$string")
    }

  implicit val codec: RomaineCodec[KeyspaceKey] = RomaineCodec.stringCodec.bimap(_.toString, parse)
}
