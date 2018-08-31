package csw.services.alarm.api.internal

import csw.services.alarm.api.internal.Separators.KeySeparator
import csw.services.alarm.api.models.Key
import csw.services.alarm.api.models.Key.AlarmKey

import scala.language.implicitConversions

trait InternalKey[T <: InternalKey[T]] {
  val value: String
  val prefix: String
  implicit def toAlarmKey: AlarmKey = AlarmKey(value.stripPrefix(prefix))
}

private[alarm] case class AckStatusKey(value: String) extends InternalKey[AckStatusKey] {
  override val prefix: String = AckStatusKey.prefix
}

private[alarm] object AckStatusKey {
  val prefix: String                                     = s"ackstatus$KeySeparator"
  implicit def fromAlarmKey(alarmKey: Key): AckStatusKey = AckStatusKey(prefix + alarmKey.value)
}

private[alarm] case class ShelveStatusKey(value: String) extends InternalKey[ShelveStatusKey] {
  override val prefix: String = ShelveStatusKey.prefix
}

private[alarm] object ShelveStatusKey {
  val prefix: String                                        = s"shelvestatus$KeySeparator"
  implicit def fromAlarmKey(alarmKey: Key): ShelveStatusKey = ShelveStatusKey(prefix + alarmKey.value)
}

private[alarm] case class AlarmTimeKey(value: String) extends InternalKey[AlarmTimeKey] {
  override val prefix: String = AlarmTimeKey.prefix
}

private[alarm] object AlarmTimeKey {
  val prefix: String                                     = s"alarmtime$KeySeparator"
  implicit def fromAlarmKey(alarmKey: Key): AlarmTimeKey = AlarmTimeKey(prefix + alarmKey.value)
}

private[alarm] case class LatchedSeverityKey(value: String) extends InternalKey[LatchedSeverityKey] {
  override val prefix: String = LatchedSeverityKey.prefix
}

private[alarm] object LatchedSeverityKey {
  val prefix: String                                           = s"latchedseverity$KeySeparator"
  implicit def fromAlarmKey(alarmKey: Key): LatchedSeverityKey = LatchedSeverityKey(prefix + alarmKey.value)
}
