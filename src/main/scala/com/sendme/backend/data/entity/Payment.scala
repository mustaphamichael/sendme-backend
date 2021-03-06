package com.sendme.backend.data.entity

import java.time.Instant

final case class Payment(
  identifier: String,
  provider: String,
  amount: Double,
  accountId: Int,
  dateCreated: Instant
) extends DefaultEntity
