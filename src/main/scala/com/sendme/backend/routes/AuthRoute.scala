package com.sendme.backend.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.sendme.backend.data.User
import com.sendme.backend.data.repository.{ AccountRepository, UserRepository }
import com.sendme.backend.util.{ FailureResponse, HashingUtil, SuccessResponse }
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class AuthRoute(
  userRepository: UserRepository,
  userAccountRepository: AccountRepository
)(implicit ec: ExecutionContext) {
  import AuthRoute._

  private val log = LoggerFactory.getLogger(getClass.getName)

  private val userRepo    = new userRepository.UserTableOps
  private val userAccount = new userAccountRepository.UserAccountTableOps

  val route: Route = pathPrefix("auth") { signUpRoute ~ loginRoute }

  private def signUpRoute: Route =
    path("signup") {
      post {
        entity(as[SignUpPayload]) { req =>
          log.info("Received request to create user [{}]", req.email)

          // hash the password
          HashingUtil.hashPassword(req.password) match {
            case Failure(exception)      =>
              log.warn(s"Encryption failed with [{}]", exception)
              complete(StatusCodes.BadRequest, FailureResponse(INVALID_REQUEST_ERROR))
            case Success(hashedPassword) =>
              // create user and account data
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
                    SuccessResponse[User](user)
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
                  complete(SuccessResponse[User](user))
              }
          }
        }
      }
    }

  private def signOutRoute: Route = ???

}

object AuthRoute {
  val DUPLICATE_USER_ERROR  = "This user already exist, login instead"
  val INVALID_REQUEST_ERROR = "The request provided is invalid"
  val UNAUTHORIZED_ERROR    = "Email or password may be invalid"

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
    userRepository: UserRepository,
    userAccountRepository: AccountRepository
  )(implicit ec: ExecutionContext): AuthRoute =
    new AuthRoute(userRepository, userAccountRepository)
}
