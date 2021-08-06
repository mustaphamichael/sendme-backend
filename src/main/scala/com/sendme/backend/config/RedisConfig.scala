package com.sendme.backend.config

import com.typesafe.config.Config

final case class RedisConfig(
  host: String,
  port: Int,
  secret: Option[String] = None
)

object RedisConfig {
  def getConfig(config: Config): RedisConfig =
    RedisConfig(
      host   = config.getString("cache.redis.host"),
      port   = config.getInt("cache.redis.port"),
      secret = Option(config.getString("cache.redis.secret"))
    )
}
