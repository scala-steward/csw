package csw.location.server.http

import akka.http.scaladsl.Http
import csw.location.helpers.LSNodeSpec
import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.location.server.internal.ServerWiring
import org.scalatest.BeforeAndAfterAll

import scala.util.Try

trait MultiNodeHTTPLocationService {
  self: LSNodeSpec[_] with BeforeAndAfterAll =>
  private val maybeBinding: Option[Http.ServerBinding] = Try {
    val binding = ServerWiring.make(self.typedSystem, enableAuth = false).locationHttpService.start()
    Some(binding.await)
  } match {
    case _ => None // ignore binding errors
  }

  override def afterAll(): Unit = {
    maybeBinding.foreach(_.unbind().await)
    multiNodeSpecAfterAll()
  }

}
