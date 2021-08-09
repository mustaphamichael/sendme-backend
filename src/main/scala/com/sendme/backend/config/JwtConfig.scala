package com.sendme.backend.config

import com.typesafe.config.Config

final case class JwtConfig(
  key: String,
  expireAt: Long,
  notBefore: Long,
  issuer: String
)

object JwtConfig {
  def fromConfig(config: Config): JwtConfig = JwtConfig(
    key       = config.getString("security.jwt.key"),
    expireAt  = config.getDuration("security.jwt.expiration").toSeconds,
    notBefore = config.getDuration("security.jwt.expire-not-before").toSeconds,
    issuer    = config.getString("security.jwt.issuer")
  )
}
