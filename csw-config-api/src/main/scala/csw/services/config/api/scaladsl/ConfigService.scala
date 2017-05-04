package csw.services.config.api.scaladsl

import java.nio.file.Path
import java.time.Instant

import csw.services.config.api.commons.FileType
import csw.services.config.api.models._

import scala.concurrent.Future

/**
 * Defines an interface for storing and retrieving configuration information
 */
trait ConfigService extends ConfigAdminService with ConfigClientService
