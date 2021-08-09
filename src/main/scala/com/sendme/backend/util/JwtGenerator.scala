package com.sendme.backend.util

import com.sendme.backend.config.JwtConfig
import pdi.jwt.{ JwtAlgorithm, JwtCirce, JwtClaim }

import java.time.Clock
import scala.util.Try

class JwtGenerator(
  config: JwtConfig
) {
  implicit val clock: Clock = Clock.systemUTC()

  private val algo = JwtAlgorithm.HS512

  def generateToken(
    id: String,
    audience: String
  ): String = {
    val claim = JwtClaim()
      .by(config.issuer)
      .to(audience)
      .about(id)
      .expiresIn(config.expireAt)
      .startsIn(-config.notBefore)
      .issuedNow
      .+("id", id)

    JwtCirce.encode(claim, config.key, algo)
  }

  def decodeToken(
    token: String
  ): Try[JwtClaim] =
    JwtCirce.decode(token, config.key, Seq(algo))
}

object JwtGenerator {
  def apply(config: JwtConfig) = new JwtGenerator(config)
}
