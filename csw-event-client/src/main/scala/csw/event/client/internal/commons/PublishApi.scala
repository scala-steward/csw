package csw.event.client.internal.commons
import akka.Done
import csw.params.events.Event

import scala.concurrent.Future

trait PublishApi {
  def publish(event: Event): Future[Done]
  def shutdown(): Future[Done]
}
