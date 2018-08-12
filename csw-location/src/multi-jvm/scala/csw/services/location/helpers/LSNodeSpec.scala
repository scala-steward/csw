package csw.services.location.helpers

import akka.http.scaladsl.Http
import akka.remote.testkit.{MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit.ImplicitSender
import csw.services.location.commons.CswCluster
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import scala.util.control.NonFatal

abstract class LSNodeSpec[T <: NMembersAndSeed](val config: T, mode: String = "cluster")
    extends MultiNodeSpec(config, config.makeSystem)
    with ImplicitSender
    with MultiNodeSpecCallbacks
    with FunSuiteLike
    with Matchers
    with BeforeAndAfterAll {

  protected val cswCluster: CswCluster = CswCluster.withSystem(system)
  protected val locationService: LocationService = mode match {
    case "http"    => LocationServiceFactory.makeLocalHttpClient(system, cswCluster.mat)
    case "cluster" => LocationServiceFactory.withCluster(cswCluster)
  }

  override def initialParticipants: Int = roles.size

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = {
    if (mode.equals("http"))
      Http().shutdownAllConnectionPools().recover { case NonFatal(e) ⇒ e.printStackTrace() }(system.dispatcher).await
    multiNodeSpecAfterAll()
  }

  test("ensure that location service is up for all the nodes") {
    locationService.list.await
    enterBarrier("cluster-formed")
  }

}
