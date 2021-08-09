package com.sendme.backend.service

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import com.sendme.backend.data.cache.CacheClient
import com.sendme.backend.util.{ FailureResponse, JwtGenerator }
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import org.slf4j.LoggerFactory

import java.time.Clock
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ Await, ExecutionContext }

class AuthMiddleware(
  cacheClient: CacheClient,
  jwtGenerator: JwtGenerator
)(implicit ec: ExecutionContext) {
  import AuthMiddleware._

  implicit val clock: Clock = Clock.systemUTC()

  private val log = LoggerFactory.getLogger(getClass.getName)

  def authenticated: Directive1[JwtContent] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case None              => complete(StatusCodes.Unauthorized, FailureResponse("Access token is missing"))
      case Some(tokenString) =>
        val token = tokenString.split(" ")(1)
        if (isTokenValid(token)) provide(parseContent(jwtGenerator.decodeToken(token).get.content, token))
        else complete(StatusCodes.Unauthorized, FailureResponse("Session expired or invalid"))
    }
  }

  private def isTokenValid(token: String): Boolean = {
    Await.result(
      for {
        d <- cacheClient.getElement(s"REVOKED_$token")
      } yield d match {
        case Some(_) => false
        case None    =>
          jwtGenerator
            .decodeToken(token)
            .fold(
              ex => {
                log.warn("Decoding jwt token failed with [{}]", ex)
                false
              },
              claim => claim.isValid
            )
      },
      10.seconds
    )
  }

  private def parseContent(jsonString: String, token: String): JwtContent = {
    parse(jsonString)
      .fold(
        _ => JwtContent.empty,
        json => {
          val tokenField = Map("token" -> token).asJson
          json
            .deepMerge(tokenField)
            .as[JwtContent]
            .getOrElse(JwtContent.empty)
        }
      )
  }
}

object AuthMiddleware {
  final case class JwtContent(id: String, token: String)
  object JwtContent {
    val empty: JwtContent = JwtContent("0", "")
  }

  def apply(
    cacheClient: CacheClient,
    jwtGenerator: JwtGenerator
  )(implicit ec: ExecutionContext) = new AuthMiddleware(cacheClient, jwtGenerator)
}
