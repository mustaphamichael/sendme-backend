package com.sendme.backend.data.cache

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait CacheClient {
  def addElement(key: String, value: String, lifetime: Option[FiniteDuration] = None): Future[Boolean]

  def getElement(key: String): Future[Option[String]]

  def removeElement(key: String): Future[Option[Long]]
}
