package csw.services

/**
 * == Location Service ==
 *
 * The Location Service implemented in this project is based on CRDT (Conflict Free Replicated Data).
 * The Location Service helps you resolve the hostname and port number for a service which can be used for further communication,
 * In case of Akka connection, It helps you to resolve component reference with which you can send messages
 * as well as other information for example, logAdminActorRef which is used to dynamically change the log level of component.
 *
 * Location Service supports three types of services as follows: Akka, Tcp and HTTP based services.
 *
 * Before calling LocationServiceFactory.make() to get an handle of LocationService,
 * it is recommended to have below environment variables set to System global environments or system properties:
 *  - interfaceName (The network interface where akka cluster is formed.) Ex. interfaceName=eth0 if not set, first inet4 interface by priority will be picked
 *  - clusterSeeds (The host address and port of the seedNodes of the cluster) Ex. clusterSeeds="192.168.121.10:3552, 192.168.121.11:3552"
 *  - clusterPort (Specify port on which to start this service) Ex. clusterPort=3552 if this property is not set, service will start on random port.
 *
 * Location service make use of classes defined in `csw-common` package, for example:
 * - [[csw.common.location.Location]] :
 *  When you resolve component location using [[csw.services.location.scaladsl.LocationService]], you get back this location.
 *  There are three types of Locations:
 *   - [[csw.common.location.AkkaLocation]]: holds hostname, port and actorRef which is ready to receive [[csw.common.commands.Command]] from outside.
 *   - [[csw.common.location.TcpLocation]]: holds hostname and port which can be used for further communication.
 *   - [[csw.common.location.HttpLocation]]: holds hostname and port which can be used for further communication.
 *
 * - [[csw.common.location.Connection]]  :
 *  Connection holds the identity of component which includes `component name` and [[csw.common.location.ComponentType]].
 *  Every component needs to register with location service using unique connection.
 *  There are three types of Connections:
 *  - [[csw.common.location.Connection.AkkaConnection]]
 *  - [[csw.common.location.Connection.TcpConnection]]
 *  - [[csw.common.location.Connection.HttpConnection]]
 *
 * Another important actor from LocationService is `DeathwatchActor` [[csw.services.location.internal.DeathwatchActor]]
 * which gets created with LocationService initialization. Job of this actor is to watch health of every component and on termination,
 * unregister terminated component from LocationService.
 *
 * Complete guide of usage of different API's provided by LocationService is available at:
 * https://tmtsoftware.github.io/csw-prod/services/location.html
 */
package object location {}
