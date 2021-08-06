package com.sendme.backend.config

import com.typesafe.config.Config

final case class SlickConfig(
  url: String,
  username: String,
  password: String,
  driver: String,
  keepAlive: Boolean
)

object SlickConfig {
  def getConfig(config: Config): SlickConfig =
    SlickConfig(
      url       = config.getString("database.url"),
      username  = config.getString("database.user"),
      password  = config.getString("database.password"),
      driver    = config.getString("database.driver"),
      keepAlive = config.getBoolean("database.keep-alive-connection")
    )
}
