package native.example

import java.nio.file.Paths
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import csw.aas.native.NativeAppAuthAdapterFactory
import csw.aas.native.api.NativeAppAuthAdapter
import csw.aas.native.scaladsl.FileAuthStore
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import scala.concurrent.ExecutionContextExecutor

object AuthWiring {
  implicit lazy val actorSystem: ActorSystem       = ActorSystem("demo-cli")
  implicit lazy val ec: ExecutionContextExecutor   = actorSystem.dispatcher
  implicit lazy val mat: Materializer              = ActorMaterializer()
  lazy val locationService: LocationService        = HttpLocationServiceFactory.makeLocalClient(actorSystem, mat)
  lazy val authStore                               = new FileAuthStore(Paths.get("/tmp/demo-cli/auth"))
  lazy val nativeAuthAdapter: NativeAppAuthAdapter = NativeAppAuthAdapterFactory.make(locationService, authStore)
}
