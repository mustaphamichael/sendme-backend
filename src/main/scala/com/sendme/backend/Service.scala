package com.sendme.backend

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route
import com.sendme.backend.config.HttpConfig
import com.sendme.backend.data.repository.{ AccountRepository, UserRepository }
import com.sendme.backend.routes.AuthRoute
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }

class Service {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "sendme-backend-app")
  implicit val ec: ExecutionContext         = system.executionContext

  private val log = LoggerFactory.getLogger(this.getClass.getName)

  private val config     = ConfigFactory.load()
  private val httpConfig = HttpConfig.getConfig(config)

  private val userRepo    = UserRepository()
  private val accountRepo = AccountRepository()

  private val authRoute = AuthRoute(userRepo, accountRepo).route

  // routes
  private val serviceRoutes: Route = pathPrefix("api") { authRoute }

  val bindFuture: Future[Http.ServerBinding] = {
    log.info("Service running on [{}]:[{}]", httpConfig.host, httpConfig.port)
    Http()
      .newServerAt(interface = httpConfig.host, port = httpConfig.port)
      .bind(serviceRoutes)
  }

  bindFuture.failed.foreach { throwable =>
    log.warn("Service shutting down because [{}]", throwable)
    system.terminate()
  }

}
