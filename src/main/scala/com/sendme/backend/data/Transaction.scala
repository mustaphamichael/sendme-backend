package com.sendme.backend.data

import java.time.Instant
import java.util.UUID

case class Transaction(
  accountId: Int,
  code: String,
  amount: Double,
  transactionType: String,
  status: String,
  description: String,
  dateCreated: Instant
) extends DefaultEntity

object Transaction {
  def create(
    accountId: Int,
    amount: Double,
    transactionType: TransactionType,
    status: TransactionStatus,
    description: String
  ): Transaction = Transaction(
    accountId       = accountId,
    code            = UUID.randomUUID().toString,
    amount          = amount,
    transactionType = transactionType.toString,
    status          = status.toString,
    description     = description,
    dateCreated     = Instant.now
  )
}

sealed trait TransactionType
case object SendMoney extends TransactionType
case object AddMoney extends TransactionType

sealed trait TransactionStatus
case object Pending extends TransactionStatus
case object Successful extends TransactionStatus
case object Failed extends TransactionStatus
