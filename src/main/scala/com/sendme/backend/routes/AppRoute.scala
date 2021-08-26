package com.sendme.backend.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.sendme.backend.config.{ JwtConfig, RedisConfig }
import com.sendme.backend.data.cache.RedisBackend
import com.sendme.backend.data.repository._
import com.sendme.backend.service.auth.AuthMiddleware.JwtContent
import com.sendme.backend.service.auth.{ AuthMiddleware, AuthService }
import com.sendme.backend.service.email.EmailClient
import com.sendme.backend.service.payment.PaymentClient
import com.sendme.backend.service.transaction.TransactionService
import com.sendme.backend.util.JwtGenerator
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

class AppRoute(
  config: Config
)(implicit actorSystem: ActorSystem[Nothing]) {

  private implicit val ec: ExecutionContext = actorSystem.executionContext

  private val redisConfig = RedisConfig.getConfig(config)
  private val jwtConfig   = JwtConfig.fromConfig(config)

  private val userRepo        = UserRepository()
  private val accountRepo     = AccountRepository()
  private val transactionRepo = TransactionRepository()
  private val paymentRepo     = PaymentRepository()
  private val jwtGenerator    = JwtGenerator(jwtConfig)
  private val cache           = RedisBackend(redisConfig)
  private val paymentClient   = PaymentClient(config, paymentRepo)
  private val emailClient     = EmailClient(config)

  private val authService    = AuthService(userRepo, accountRepo, jwtGenerator, cache, emailClient)
  private val authMiddleware = AuthMiddleware(cache, jwtGenerator)
  private val authRoute      = AuthRoute(authService)

  private val transactionService = TransactionService(userRepo, accountRepo, transactionRepo, paymentClient)
  private val transactionRoute   = TransactionRoute(transactionService)

  val routes: Route = pathPrefix("api") {
    concat(authRoute.route, protectedRoutes)
  }

  // all protected routes that require authorization should be passed here
  private def protectedRoutes: Route =
    authMiddleware.authenticated { jwtContent =>
      passUserDataToRequest(jwtContent) {
        concat(authRoute.logout, transactionRoute.route)
      }
    }

  private def passUserDataToRequest(content: JwtContent) =
    mapRequest(_.withHeaders(RawHeader("id", content.id), RawHeader("token", content.token)))

}

object AppRoute {
  def apply(
    config: Config
  )(implicit actorSystem: ActorSystem[Nothing]) = new AppRoute(config)
}
