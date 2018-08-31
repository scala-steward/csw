package csw.services.alarm.api.internal

import csw.services.alarm.api.models.Key
import csw.services.alarm.api.internal.Separators.KeySeparator

import scala.language.implicitConversions

private[alarm] case class SeverityKey(value: String) extends InternalKey[SeverityKey] {
  override val prefix: String = SeverityKey.prefix
}

private[alarm] object SeverityKey {
  val prefix: String                                         = s"severity$KeySeparator"
  implicit def fromAlarmKey(alarmKey: Key): SeverityKey      = SeverityKey(prefix + alarmKey.value)
  def fromMetadataKey(metadataKey: MetadataKey): SeverityKey = fromAlarmKey(metadataKey.toAlarmKey)
}
