package com.sendme.backend.service.payment

import scala.concurrent.Future

trait PaymentProvider[I, O] {
  def name: String

  def receivePayment(payload: I): Future[O]

  def makeTransfer(payload: I): Future[O]
}
