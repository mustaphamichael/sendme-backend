package com.sendme.backend.data.entity

import java.time.Instant
import java.util.UUID

case class Transaction(
  accountId: Int,
  userId: Int,
  code: String,
  amount: Double,
  transactionType: String,
  status: String,
  description: String,
  dateCreated: Instant
) extends DefaultEntity

object Transaction {
  def create(
    account: Account,
    amount: Double,
    transactionType: TransactionType,
    status: TransactionStatus,
    description: String
  ): Transaction = Transaction(
    accountId       = account.id.getOrElse(0),
    userId          = account.userId,
    code            = UUID.randomUUID().toString.toUpperCase,
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
