package csw.services.config.client.internal

import java.nio.{file ⇒ jnio}

import csw.services.config.api.models.{JsonSupport, _}
import csw.services.config.api.scaladsl.ConfigClientService

import scala.concurrent.Future

class ConfigClient(configServiceResolver: ConfigServiceResolver, actorRuntime: ActorRuntime)
    extends ConfigClientService
    with JsonSupport {

  private val configAdminClient: ConfigAdminClient = new ConfigAdminClient(configServiceResolver, actorRuntime)

  override def getActive(path: jnio.Path): Future[Option[ConfigData]] = configAdminClient.getActive(path)

  override def exists(path: jnio.Path, id: Option[ConfigId]): Future[Boolean] = configAdminClient.exists(path, id)
}
