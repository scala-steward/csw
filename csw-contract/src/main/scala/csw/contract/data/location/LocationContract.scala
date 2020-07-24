package csw.contract.data.location

import akka.Done
import csw.contract.ResourceFetcher
import csw.contract.generator.ClassNameHelpers._
import csw.contract.generator._
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.exceptions._
import csw.location.api.messages.LocationHttpMessage._
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.messages.{LocationHttpMessage, LocationWebsocketMessage}
import csw.location.api.models._
import csw.prefix.models.Subsystem

object LocationContract extends LocationData with LocationServiceCodecs {
  private val models: ModelSet = ModelSet.models(
    ModelType(akkaRegistration, httpRegistration, publicHttpRegistration, tcpRegistration),
    ModelType(akkaLocation, httpLocation, tcpLocation),
    ModelType(locationUpdated, locationRemoved),
    ModelType(ConnectionType),
    ModelType[Connection](akkaConnection, httpConnection, tcpConnection),
    ModelType(ComponentId(prefix, ComponentType.HCD)),
    ModelType(ComponentType),
    ModelType(
      registrationFailed,
      otherLocationIsRegistered,
      unregisterFailed,
      registrationListingFailed
    ),
    ModelType(Subsystem),
    ModelType(prefix)
  )

  private val httpEndpoints: List[Endpoint] = List(
    Endpoint(name[Register], name[Location], List(name[RegistrationFailed], name[OtherLocationIsRegistered])),
    Endpoint(name[Unregister], name[Done], List(name[UnregistrationFailed])),
    Endpoint(objectName(UnregisterAll), name[Done], List(name[UnregistrationFailed])),
    Endpoint(name[Find], arrayName[Location]),
    Endpoint(name[Resolve], arrayName[Location]),
    Endpoint(objectName(ListEntries), arrayName[Location], List(name[RegistrationListingFailed])),
    Endpoint(name[ListByComponentType], arrayName[Location], List(name[RegistrationListingFailed])),
    Endpoint(name[ListByConnectionType], arrayName[Location], List(name[RegistrationListingFailed])),
    Endpoint(name[ListByHostname], arrayName[Location], List(name[RegistrationListingFailed])),
    Endpoint(name[ListByPrefix], arrayName[Location], List(name[RegistrationListingFailed]))
  )

  private val httpRequests: ModelSet = ModelSet.requests[LocationHttpMessage](
    ModelType(akkaRegister, httpRegister, publicHttpRegister),
    ModelType(unregister),
    ModelType(unregisterAll),
    ModelType(find),
    ModelType(resolve),
    ModelType(listEntries),
    ModelType(listByComponentTypeHcd, listByComponentTypeAssembly),
    ModelType(listByAkkaConnectionType, listByHttpConnectionType),
    ModelType(listByHostname),
    ModelType(listByPrefix)
  )

  private val webSocketEndpoints: List[Endpoint] = List(
    Endpoint(name[Track], name[TrackingEvent])
  )

  private val websocketRequests: ModelSet = ModelSet.requests[LocationWebsocketMessage](
    ModelType(track)
  )

  private val readme: Readme = Readme(ResourceFetcher.getResourceAsString("location-service/README.md"))

  val service: Service = Service(
    Contract(httpEndpoints, httpRequests),
    Contract(webSocketEndpoints, websocketRequests),
    models,
    readme
  )
}
