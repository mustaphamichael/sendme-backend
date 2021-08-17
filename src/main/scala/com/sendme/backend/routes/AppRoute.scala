package com.sendme.backend.routes

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.sendme.backend.config.{ JwtConfig, RedisConfig }
import com.sendme.backend.data.cache.RedisBackend
import com.sendme.backend.data.repository.{ AccountRepository, PaymentRepository, TransactionRepository, UserRepository }
import com.sendme.backend.service.AuthMiddleware
import com.sendme.backend.service.AuthMiddleware.JwtContent
import com.sendme.backend.service.payment.PaymentClient
import com.sendme.backend.service.transaction.TransactionService
import com.sendme.backend.util.JwtGenerator
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

class AppRoute(
  config: Config
)(implicit ec: ExecutionContext) {

  private val redisConfig = RedisConfig.getConfig(config)
  private val jwtConfig   = JwtConfig.fromConfig(config)

  private val userRepo        = UserRepository()
  private val accountRepo     = AccountRepository()
  private val transactionRepo = TransactionRepository()
  private val paymentRepo     = PaymentRepository()
  private val jwtGenerator    = JwtGenerator(jwtConfig)
  private val cache           = RedisBackend(redisConfig)
  private val paymentClient   = PaymentClient(config, paymentRepo)

  val routes: Route = pathPrefix("api") {
    concat(authRoute.route, protectedRoutes)
  }

  private val authRoute          = AuthRoute(userRepo, accountRepo, jwtGenerator, cache)
  private val authMiddleware     = AuthMiddleware(cache, jwtGenerator)
  private val transactionService = TransactionService(userRepo, accountRepo, transactionRepo, paymentClient)
  private val transactionRoute   = TransactionRoute(transactionService)

  // all protected routes that require authorization should be passed here
  private def protectedRoutes: Route =
    authMiddleware.authenticated { jwtContent =>
      passUserIdToRequest(jwtContent) {
        concat(authRoute.logout, transactionRoute.route)
      }
    }

  private def passUserIdToRequest(content: JwtContent) =
    mapRequest(_.withHeaders(RawHeader("id", content.id), RawHeader("token", content.token)))

}

object AppRoute {
  def apply(
    config: Config
  )(implicit ec: ExecutionContext) = new AppRoute(config)
}
