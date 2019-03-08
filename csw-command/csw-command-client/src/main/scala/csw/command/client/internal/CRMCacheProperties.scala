package csw.command.client.internal

import java.time.Duration

import com.typesafe.config.{Config, ConfigFactory}

private[internal] case class CRMCacheProperties(maxSize: Int, expiry: Duration)

private[internal] object CRMCacheProperties {
  def apply(config: Config = ConfigFactory.load()): CRMCacheProperties = {
    val crmConfig = config.getConfig("csw-command-client.command-response-state")
    CRMCacheProperties(crmConfig.getInt("maximum-size"), crmConfig.getDuration("expiry"))
  }
}
