package csw.aas.http

import java.net.URI

import akka.http.javadsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.ConfigValueFactory
import csw.aas.core.commons.AASConnection
import csw.aas.http.AuthorizationPolicy.{CustomPolicy, RealmRolePolicy}
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{HttpLocation, Metadata}
import csw.location.api.scaladsl.LocationService
import msocket.security.AccessControllerFactory
import msocket.security.api.TokenValidator
import msocket.security.models.AccessToken
import org.mockito.ArgumentMatchersSugar._
import org.mockito.MockitoSugar
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class SecurityDirectivesTest extends AnyFunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("secure using customPolicy should return 200 OK when policy matches | DEOPSCSW-579") {
    val tokenValidator     = mock[TokenValidator]
    val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), "TMT")
    import securityDirectives._

    val validTokenWithPolicyMatchStr    = "validTokenWithPolicyMatch"
    val validTokenWithPolicyMatchHeader = Authorization(OAuth2BearerToken(validTokenWithPolicyMatchStr))

    val validTokenWithPolicyMatch = mock[AccessToken]

    when(tokenValidator.validate(validTokenWithPolicyMatchStr)).thenReturn(Future.successful(validTokenWithPolicyMatch))

    val route: Route = post {
      secure(CustomPolicy(_ => true)) { _ => complete("OK") }
    }

    Post("/").addHeader(validTokenWithPolicyMatchHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("secure using customPolicy should return 200 OK when token not passed and auth is disabled | DEOPSCSW-579") {
    val tokenValidator     = mock[TokenValidator]
    val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, false), "TMT")
    import securityDirectives._

    val route: Route = post {
      secure(CustomPolicy(_ => false)) { _ => complete("OK") }
    }

    Post("/") ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("sGet using customPolicy should return 200 OK when policy matches | DEOPSCSW-579") {
    val tokenValidator     = mock[TokenValidator]
    val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), "TMT")
    import securityDirectives._

    val validTokenWithPolicyMatchStr    = "validTokenWithPolicyMatch"
    val validTokenWithPolicyMatchHeader = Authorization(OAuth2BearerToken(validTokenWithPolicyMatchStr))

    val validTokenWithPolicyMatch = mock[AccessToken]

    when(tokenValidator.validate(validTokenWithPolicyMatchStr)).thenReturn(Future.successful(validTokenWithPolicyMatch))

    val route: Route = sGet(CustomPolicy(_ => true)) { _ => complete("OK") }

    Get("/").addHeader(validTokenWithPolicyMatchHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("sPost using realmRole should return 200 OK when token is valid & has realmRole | DEOPSCSW-579") {
    val tokenValidator     = mock[TokenValidator]
    val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), "TMT")
    import securityDirectives._

    val validTokenWithRealmRoleStr    = "validTokenWithRealmRoleStr"
    val validTokenWithRealmRole       = mock[AccessToken]
    val validTokenWithRealmRoleHeader = Authorization(OAuth2BearerToken(validTokenWithRealmRoleStr))
    when(validTokenWithRealmRole.hasRealmRole("admin")).thenReturn(true)

    when(tokenValidator.validate(validTokenWithRealmRoleStr)).thenReturn(Future.successful(validTokenWithRealmRole))

    val route: Route = sPost(RealmRolePolicy("admin")) { _ => complete("OK") }

    Post("/").addHeader(validTokenWithRealmRoleHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("sDelete using realmRole should return 200 OK when token is valid & has realmRole | DEOPSCSW-579") {
    val tokenValidator     = mock[TokenValidator]
    val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), "TMT")
    import securityDirectives._

    val validTokenWithRealmRoleStr    = "validTokenWithRealmRoleStr"
    val validTokenWithRealmRole       = mock[AccessToken]
    val validTokenWithRealmRoleHeader = Authorization(OAuth2BearerToken(validTokenWithRealmRoleStr))
    when(validTokenWithRealmRole.hasRealmRole("admin")).thenReturn(true)

    when(tokenValidator.validate(validTokenWithRealmRoleStr)).thenReturn(Future.successful(validTokenWithRealmRole))

    val route: Route = sDelete(RealmRolePolicy("admin")) { _ => complete("OK") }

    Delete("/").addHeader(validTokenWithRealmRoleHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("sHead using realmRole should return 200 OK when token is valid & has realmRole | DEOPSCSW-579") {
    val tokenValidator     = mock[TokenValidator]
    val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), "TMT")
    import securityDirectives._

    val validTokenWithRealmRoleStr    = "validTokenWithRealmRoleStr"
    val validTokenWithRealmRole       = mock[AccessToken]
    val validTokenWithRealmRoleHeader = Authorization(OAuth2BearerToken(validTokenWithRealmRoleStr))
    when(validTokenWithRealmRole.hasRealmRole("admin")).thenReturn(true)

    when(tokenValidator.validate(validTokenWithRealmRoleStr)).thenReturn(Future.successful(validTokenWithRealmRole))

    val route: Route = sHead(RealmRolePolicy("admin")) { _ => complete("OK") }

    Head("/").addHeader(validTokenWithRealmRoleHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("sPatch using customPolicy should return AuthenticationFailedRejection when token is not present | DEOPSCSW-579") {
    val tokenValidator     = mock[TokenValidator]
    val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), "TMT")
    import securityDirectives._

    val route: Route = sPatch(CustomPolicy(_ => false)) { _ => complete("OK") }

    Patch("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("apply should not resolve AAS location when auth param is disabled | DEOPSCSW-579") {
    val locationService: LocationService = mock[LocationService]
    SecurityDirectives(system.settings.config, locationService, enableAuth = false)
    verify(locationService, never).resolve(any[HttpConnection], any[FiniteDuration])
  }

  test("apply should resolve AAS location when auth param is enabled | DEOPSCSW-579") {
    val locationService: LocationService = mock[LocationService]
    when(locationService.resolve(any[HttpConnection], any[FiniteDuration]))
      .thenReturn(Future.successful(Some(HttpLocation(AASConnection.value, URI.create(""), Metadata.empty))))
    SecurityDirectives(system.settings.config, locationService, enableAuth = true)
    verify(locationService).resolve(any[HttpConnection], any[FiniteDuration])
  }

  test("authDisabled should never resolve AAS location | DEOPSCSW-579") {
    val locationService: LocationService = mock[LocationService]
    SecurityDirectives.authDisabled(system.settings.config)
    verify(locationService, never).resolve(any[HttpConnection], any[FiniteDuration])
  }

  test("apply should not resolve AAS location when auth is disabled in config | DEOPSCSW-579") {
    val locationService: LocationService = mock[LocationService]
    val config                           = system.settings.config.withValue("auth-config.disabled", ConfigValueFactory.fromAnyRef("true"))
    SecurityDirectives(config, locationService)
    verify(locationService, never).resolve(any[HttpConnection], any[FiniteDuration])
  }

  test("apply should resolve AAS location when auth is enabled in config | DEOPSCSW-579") {
    val locationService: LocationService = mock[LocationService]
    val config                           = system.settings.config.withValue("auth-config.disabled", ConfigValueFactory.fromAnyRef("false"))
    when(locationService.resolve(any[HttpConnection], any[FiniteDuration]))
      .thenReturn(Future.successful(Some(HttpLocation(AASConnection.value, URI.create(""), Metadata.empty))))
    SecurityDirectives(config, locationService)
    verify(locationService).resolve(any[HttpConnection], any[FiniteDuration])
  }
}
