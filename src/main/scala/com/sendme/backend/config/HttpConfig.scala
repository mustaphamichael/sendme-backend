package com.sendme.backend.config

import com.typesafe.config.Config

final case class HttpConfig(
  host: String,
  port: Int
)

object HttpConfig {
  def getConfig(config: Config): HttpConfig =
    HttpConfig(
      host = config.getString("http.host"),
      port = config.getInt("http.port")
    )
}
