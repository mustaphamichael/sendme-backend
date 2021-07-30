package com.sendme.backend.data

import java.time.Instant

trait DefaultEntity {
  def id: Option[Int]    = None
  def createdAt: Instant = Instant.now
  def updatedAt: Instant = Instant.now
}
