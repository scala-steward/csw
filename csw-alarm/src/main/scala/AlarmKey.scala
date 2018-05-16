case class AlarmKey(source: Prefix, alarmName: AlarmName) {
  val key                       = s"${source.prefix}.$alarmName"
  override def toString: String = key
}
