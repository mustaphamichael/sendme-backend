package com.sendme.backend.data.cache

import com.redis._
import com.sendme.backend.config.RedisConfig

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }

class RedisBackend(
  config: RedisConfig
)(implicit ec: ExecutionContext)
    extends CacheClient {
  private val redisClient = new RedisClient(
    host   = config.host,
    port   = config.port,
    secret = config.secret
  )

  override def addElement(
    key: String,
    value: String,
    lifetime: Option[FiniteDuration]
  ): Future[Boolean] = Future {
    lifetime match {
      case Some(time) =>
        redisClient.set(
          key    = key,
          value  = value,
          expire = time
        )
      case None       => redisClient.set(key, value)
    }
  }

  override def getElement(key: String): Future[Option[String]] = Future {
    redisClient.get(key)
  }

  override def removeElement(key: String): Future[Option[Long]] = Future {
    redisClient.del(key)
  }

}

object RedisBackend {
  def apply(
    config: RedisConfig
  )(implicit ec: ExecutionContext) = new RedisBackend(config)
}
