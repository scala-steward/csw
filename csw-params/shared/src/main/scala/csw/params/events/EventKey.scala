package csw.params.events

import csw.prefix.models.Prefix

/**
 * A wrapper class representing the key for an event e.g. tcs.prog.cloudcover.oiwfsProbeDemands
 *
 * @param source represents the prefix of the component that publishes this event
 * @param eventName represents the name of the event
 */
case class EventKey(source: Prefix, eventName: EventName) {
  val key                       = s"${source}${EventKey.SEPARATOR}$eventName"
  override def toString: String = key
}

object EventKey {
  private val SEPARATOR = "."

  def apply(eventKeyStr: String): EventKey = {
    require(eventKeyStr != null)
    val strings = eventKeyStr.splitAt(eventKeyStr.lastIndexOf(SEPARATOR))
    new EventKey(Prefix(strings._1), EventName(strings._2.tail))
  }
}
