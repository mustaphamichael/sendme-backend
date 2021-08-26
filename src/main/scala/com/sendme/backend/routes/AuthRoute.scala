package com.sendme.backend.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.sendme.backend.data.entity.User
import com.sendme.backend.service.auth.AuthService
import com.sendme.backend.util.{ ApiException, FailureResponse, MessagePayload, SuccessResponse }
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class AuthRoute(
  authService: AuthService
)(implicit ec: ExecutionContext) {
  import AuthRoute._

  private val log = LoggerFactory.getLogger(getClass.getName)

  val route: Route  = pathPrefix("auth") { signUpRoute ~ loginRoute }
  val logout: Route = pathPrefix("auth") { logoutRoute } // protected route

  private def signUpRoute: Route =
    path("signup") {
      post {
        entity(as[SignUpPayload]) { req =>
          log.info("Received request to create user [{}]", req.email)

          onComplete(
            authService.signup(User.fromRequest(req))
          ) {
            case Success(response)                      =>
              complete(StatusCodes.Created, SuccessResponse(response))
            case Failure(ApiException(status, message)) =>
              complete(status, FailureResponse(message))
          }
        }
      }
    }

  private def loginRoute: Route =
    path("login") {
      post {
        entity(as[LoginPayload]) { req =>
          log.info("Received request to login user [{}]", req.email)

          onComplete(
            authService.login(req.email, req.password)
          ) {
            case Success(response)                      =>
              complete(SuccessResponse(response))
            case Failure(ApiException(status, message)) =>
              complete(status, FailureResponse(message))
          }
        }
      }
    }

  /* Add the token to the cache as a blacklist */
  private def logoutRoute: Route =
    path("logout") {
      get {
        log.info("Received request to logout user")

        headerValueByName("token") { token =>
          onComplete(
            authService.logout(token)
          ) {
            case Success(message)                       =>
              complete(MessagePayload(message))
            case Failure(ApiException(status, message)) =>
              complete(status, FailureResponse(message))
          }
        }
      }
    }

}

object AuthRoute {
  final case class SignUpPayload(
    name: String,
    email: String,
    password: String
  )

  final case class LoginPayload(
    email: String,
    password: String
  )

  def apply(
    authService: AuthService
  )(implicit ec: ExecutionContext): AuthRoute = new AuthRoute(authService)
}
