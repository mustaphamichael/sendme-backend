package com.sendme.backend.service.auth

import akka.http.scaladsl.model.StatusCodes
import com.sendme.backend.data.cache.CacheClient
import com.sendme.backend.data.entity.User
import com.sendme.backend.data.repository.{ AccountRepository, UserRepository }
import com.sendme.backend.service.email.EmailClient
import com.sendme.backend.util.{ ApiException, HashingUtil, JwtGenerator }
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

class AuthService(
  userRepository: UserRepository,
  userAccountRepository: AccountRepository,
  jwtGenerator: JwtGenerator,
  cacheClient: CacheClient,
  emailClient: EmailClient
)(implicit ec: ExecutionContext) {
  import AuthService._

  private val log = LoggerFactory.getLogger(getClass.getName)

  private val userRepo    = new userRepository.UserTableOps
  private val userAccount = new userAccountRepository.UserAccountTableOps

  def signup(user: User): Future[AuthResponse] = {
    HashingUtil.hashPassword(user.password) match {
      case Failure(exception)      =>
        log.warn(s"Encryption failed with [{}]", exception)
        Future.failed(ApiException(StatusCodes.BadRequest, INVALID_REQUEST_ERROR))
      case Success(hashedPassword) =>
        (for {
          user <- userRepo.create(user.copy(password = hashedPassword))
          _ <- userAccount.create(User.createAccount(user))
        } yield user)
          .recoverWith { case NonFatal(exception) =>
            log.warn("Signup failed with [{}]", exception)
            Future.failed(ApiException(StatusCodes.BadRequest, DUPLICATE_USER_ERROR))
          }
          .flatMap { user =>
            // send welcome email
            emailClient
              .sendMail(
                to      = user.email,
                subject = "Welcome to SendMe",
                content = s"Dear ${user.name},\n\nThank you for registering on our platform"
              )

            Future.successful(AuthResponse.generateToken(user, jwtGenerator))
          }
    }
  }

  def login(email: String, password: String): Future[AuthResponse] = {
    userRepo
      .authenticate(email, password)
      .recoverWith { case NonFatal(exception) =>
        log.warn(s"Attempt to authenticate user [{}] failed with", email, exception)
        Future.failed(ApiException(StatusCodes.Unauthorized, UNAUTHORIZED_ERROR))
      }
      .flatMap {
        case None       =>
          Future.failed(ApiException(StatusCodes.Unauthorized, UNAUTHORIZED_ERROR))
        case Some(user) =>
          Future.successful(AuthResponse.generateToken(user, jwtGenerator))
      }
  }

  def logout(token: String): Future[String] = {
    cacheClient
      .addElement(s"REVOKED_$token", "1", Some(1.hour))
      .recoverWith { case NonFatal(exception) =>
        log.warn("Something went wrong adding user token to blacklist with [{}]", exception)
        Future.failed(ApiException(StatusCodes.InternalServerError, "Logout failed"))
      }
      .flatMap { isBlacklisted =>
        Future.successful(
          if (isBlacklisted) "Success"
          else "Already logged out"
        )
      }
  }
}

object AuthService {
  val DUPLICATE_USER_ERROR  = "This user already exist, login instead"
  val INVALID_REQUEST_ERROR = "The request provided is invalid"
  val UNAUTHORIZED_ERROR    = "Email or password may be invalid"

  final case class AuthResponse(access_token: String)
  object AuthResponse {
    def generateToken(
      user: User,
      jwtGenerator: JwtGenerator
    ): AuthResponse = AuthResponse(jwtGenerator.generateToken(user.id.getOrElse(0).toString, "User"))
  }

  def apply(
    userRepository: UserRepository,
    userAccountRepository: AccountRepository,
    jwtGenerator: JwtGenerator,
    cacheClient: CacheClient,
    emailClient: EmailClient
  )(implicit ec: ExecutionContext) =
    new AuthService(userRepository, userAccountRepository, jwtGenerator, cacheClient, emailClient)
}
