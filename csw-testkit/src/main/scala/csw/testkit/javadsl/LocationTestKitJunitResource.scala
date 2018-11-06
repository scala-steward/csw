package csw.testkit.javadsl

import csw.testkit.{LocationTestKit, TestKitSettings}
import org.junit.rules.ExternalResource

/**
 * A Junit external resource for the [[LocationTestKit]], making it possible to have Junit manage the lifecycle of the testkit.
 * The testkit will be automatically shut down when the test completes or fails.
 *
 * Example:
 * {{{
 * public class JLocationExampleTest {
 *
 *  @ClassRule
 *  public static final LocationTestKitJunitResource testKit = new LocationTestKitJunitResource();
 *
 * }
 * }}}
 */
final class LocationTestKitJunitResource(val locationTestKit: LocationTestKit) extends ExternalResource {

  /** Initialize testkit with default configuration */
  def this() = this(LocationTestKit())

  /** Initialize testkit with custom TestKitSettings */
  def this(testKitSettings: TestKitSettings) = this(LocationTestKit(testKitSettings))

  /** Start LocationTestKit */
  override def before(): Unit = locationTestKit.startLocationServer()

  /** Shuts down the LocationTestKit */
  override def after(): Unit = locationTestKit.shutdownLocationServer()

}
