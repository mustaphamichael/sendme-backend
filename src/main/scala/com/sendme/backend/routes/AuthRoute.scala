package com.sendme.backend.routes

import akka.http.javadsl.model.headers.Authorization
import akka.http.scaladsl.model.{ HttpHeader, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.sendme.backend.data.cache.CacheClient
import com.sendme.backend.data.{ User, UserResponsePayload }
import com.sendme.backend.data.repository.{ AccountRepository, UserRepository }
import com.sendme.backend.util.{ FailureResponse, HashingUtil, JwtGenerator, MessagePayload, SuccessResponse }
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success }

class AuthRoute(
  userRepository: UserRepository,
  userAccountRepository: AccountRepository,
  cacheClient: CacheClient
)(implicit ec: ExecutionContext) {
  import AuthRoute._

  private val log = LoggerFactory.getLogger(getClass.getName)

  private val userRepo    = new userRepository.UserTableOps
  private val userAccount = new userAccountRepository.UserAccountTableOps

  val route: Route = pathPrefix("auth") { signUpRoute ~ loginRoute ~ logoutRoute }

  private def signUpRoute: Route =
    path("signup") {
      post {
        entity(as[SignUpPayload]) { req =>
          log.info("Received request to create user [{}]", req.email)

          HashingUtil.hashPassword(req.password) match {
            case Failure(exception)      =>
              log.warn(s"Encryption failed with [{}]", exception)
              complete(StatusCodes.BadRequest, FailureResponse(INVALID_REQUEST_ERROR))
            case Success(hashedPassword) =>
              onComplete(for {
                user <- userRepo.create(User.fromRequest(req.copy(password = hashedPassword)))
                _ <- userAccount.create(User.createAccount(user))
              } yield user) {
                case Failure(exception) =>
                  log.warn("Signup failed with [{}]", exception)
                  complete(StatusCodes.BadRequest, FailureResponse(DUPLICATE_USER_ERROR))
                case Success(user)      =>
                  complete(
                    StatusCodes.Created,
                    SuccessResponse[AuthResponsePayload](
                      data = AuthResponsePayload(
                        user = User.response(user),
                        auth = Auth.response(user)
                      )
                    )
                  )
              }
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
            userRepo.authenticate(req.email, req.password)
          ) {
            case Failure(exception) =>
              log.warn(s"Attempt to authenticate user [{}] failed with", req.email, exception)
              complete(StatusCodes.Unauthorized, FailureResponse(UNAUTHORIZED_ERROR))
            case Success(value)     =>
              value match {
                case None       =>
                  complete(StatusCodes.Unauthorized, FailureResponse(UNAUTHORIZED_ERROR))
                case Some(user) =>
                  complete(
                    SuccessResponse[AuthResponsePayload](
                      data = AuthResponsePayload(
                        user = User.response(user),
                        auth = Auth.response(user)
                      )
                    )
                  )
              }
          }
        }
      }
    }

  /* Add the token to the cache as a blacklist */
  private def logoutRoute: Route =
    path("logout") {
      get {
        // TODO: Extract 'email' claim from jwt
        log.info("Received request to logout user")

        headerValue(extractAuthToken) { token =>
          onComplete(
            cacheClient.addElement(s"REVOKED_$token", "1", Some(1.hour))
          ) {
            case Failure(exception)     =>
              log.warn("Something went wrong adding user token to blacklist with [{}]", exception)
              complete(StatusCodes.InternalServerError, FailureResponse("Logout failed"))
            case Success(isBlacklisted) =>
              if (isBlacklisted) complete(MessagePayload())
              else complete(MessagePayload("Already logged out"))
          }
        }
      }
    }

  private def extractAuthToken: HttpHeader => Option[String] = {
    case auth: Authorization => Some(auth.value())
    case _                   => None
  }
}

object AuthRoute {
  val DUPLICATE_USER_ERROR  = "This user already exist, login instead"
  val INVALID_REQUEST_ERROR = "The request provided is invalid"
  val UNAUTHORIZED_ERROR    = "Email or password may be invalid"

  sealed trait IncomingPayload
  sealed trait OutgoingPayload

  final case class SignUpPayload(
    name: String,
    email: String,
    password: String
  ) extends IncomingPayload

  final case class LoginPayload(
    email: String,
    password: String
  ) extends IncomingPayload

  case class Auth(
    access_token: String
  ) extends OutgoingPayload

  object Auth {
    def response(
      user: User
    ): Auth = Auth(
      access_token = JwtGenerator.generateToken(user.email, "User")
    )
  }

  final case class AuthResponsePayload(
    user: UserResponsePayload,
    auth: Auth
  ) extends OutgoingPayload

  def apply(
    userRepository: UserRepository,
    userAccountRepository: AccountRepository,
    cacheClient: CacheClient
  )(implicit ec: ExecutionContext): AuthRoute =
    new AuthRoute(userRepository, userAccountRepository, cacheClient)
}
