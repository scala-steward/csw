package csw.services.event.scaladsl

import csw.messages.ccs.events.Event

trait EventService {

  def publishEvent(event: Event)

}
