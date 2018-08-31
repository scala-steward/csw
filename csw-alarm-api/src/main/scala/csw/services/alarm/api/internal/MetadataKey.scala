package csw.services.alarm.api.internal

import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.internal.Separators.KeySeparator
import csw.services.alarm.api.models.Key

import scala.language.implicitConversions

private[alarm] case class MetadataKey(value: String) extends InternalKey[MetadataKey] {
  override val prefix: String = MetadataKey.prefix
}

private[alarm] object MetadataKey {
  private val prefix                                    = s"metadata$KeySeparator"
  implicit def fromAlarmKey(alarmKey: Key): MetadataKey = MetadataKey(prefix + alarmKey.value)
}
