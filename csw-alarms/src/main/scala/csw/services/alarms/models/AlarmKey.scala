package csw.services.alarms.models

object AlarmKey {

  private[alarms] val alarmKeyPrefix = "alarm::"

  private[alarms] val severityKeyPrefix = "severity::"

  private[alarms] val alarmStateKeyPrefix = "astate::"

  private val sep = ':'

  def apply(a: AlarmEntity): AlarmKey = AlarmKey(a.subsystem, a.component, a.name)

  def apply(subsystemOpt: Option[String] = None,
            componentOpt: Option[String] = None,
            nameOpt: Option[String] = None): AlarmKey = {
    val subsystem = subsystemOpt.getOrElse("*")
    val component = componentOpt.getOrElse("*")
    val name      = nameOpt.getOrElse("*")
    AlarmKey(subsystem, component, name)
  }

  def apply(key: String): AlarmKey = {
    val k                                   = if (key.contains("::")) key.substring(key.indexOf("::") + 2) else key
    val subsystem :: component :: name :: _ = k.split(sep).toList
    AlarmKey(subsystem, component, name)
  }
}

case class AlarmKey(subsystem: String, component: String, name: String) {
  import AlarmKey._
  private val k = s"$subsystem$sep$component$sep$name"

  val key: String         = alarmKeyPrefix + k
  val severityKey: String = severityKeyPrefix + k
  val stateKey: String    = alarmStateKeyPrefix + k
}
