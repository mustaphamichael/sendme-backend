package com.sendme.backend.util

import com.typesafe.config.ConfigFactory
import pdi.jwt.{ JwtAlgorithm, JwtCirce, JwtClaim }

import java.time.Clock
import scala.util.Try

object JwtGenerator {
  implicit val clock: Clock = Clock.systemUTC()

  private val config    = ConfigFactory.load().getConfig("security.jwt")
  private val key       = config.getString("key")
  private val algo      = JwtAlgorithm.HS512
  private val expireAt  = config.getDuration("expiration").toSeconds
  private val notBefore = config.getDuration("expire-not-before").toSeconds
  private val issuer    = config.getString("issuer")

  def generateToken(
    email: String,
    audience: String
  ): String = {
    val claim = JwtClaim()
      .by(issuer)
      .to(audience)
      .about(email)
      .expiresIn(expireAt)
      .startsIn(-notBefore)
      .issuedNow
      .+("email", email)

    JwtCirce.encode(claim, key, algo)
  }

  def decodeToken(
    token: String
  ): Try[JwtClaim] =
    JwtCirce.decode(token, key, Seq(algo))
}
