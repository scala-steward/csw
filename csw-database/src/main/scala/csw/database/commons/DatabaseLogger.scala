package csw.database.commons

import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW

private[database] object DatabaseLogger extends LoggerFactory(Prefix(CSW, "database_client"))
