package csw.location.server.internal
import akka.actor.typed.ActorRef
import csw.location.api.models.{AkkaLocation, Location}

final case class LocalAkkaLocations(private val _locations: Set[AkkaLocation]) {
  def all: Set[AkkaLocation] = _locations

  def diff(other: LocalAkkaLocations): LocalAkkaLocations             = copy(_locations diff other._locations)
  def union(other: LocalAkkaLocations): LocalAkkaLocations            = copy(_locations union other._locations)
  def remove(maybeLocation: Option[AkkaLocation]): LocalAkkaLocations = copy(_locations.filterNot(maybeLocation.contains))
  def locationOf(actorRef: ActorRef[Nothing]): Option[AkkaLocation]   = _locations.find(_.actorRef == actorRef)

}
object LocalAkkaLocations {
  def empty: LocalAkkaLocations = LocalAkkaLocations(Set.empty[AkkaLocation])

  // Ignore HttpLocation, TcpLocation and remote AkkaLocations (Do not watch)
  def apply(_locations: Set[Location], hostname: String): LocalAkkaLocations = {
    val akkaLocations = _locations.collect {
      case akkaLocation: AkkaLocation if akkaLocation.uri.getHost == hostname ⇒ akkaLocation
    }

    LocalAkkaLocations(akkaLocations)
  }
}
