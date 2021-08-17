package com.sendme.backend.service.payment.providers

import com.sendme.backend.service.payment.PaymentProfile.{ PaymentRequest, PaymentResponse }
import com.sendme.backend.service.payment.PaymentProvider
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/** Demo provider to mimic payment service */
class DemoProvider extends PaymentProvider[PaymentRequest, PaymentResponse] {

  private val log = LoggerFactory.getLogger(getClass.getName)

  override def name: String = "demo-payment-provider"

  override def receivePayment(payload: PaymentRequest): Future[PaymentResponse] = {
    val (amount, userId) = (payload.amount, payload.account.userId)
    log.info("Receiving payment of [{}] from [{}]", amount, userId)
    Future.successful(
      PaymentResponse(message = "Payment received successfully", amount = amount)
    )
  }

  override def makeTransfer(payload: PaymentRequest): Future[PaymentResponse] = {
    val (amount, userId) = (payload.amount, payload.account.userId)
    log.info("Making a transfer of [{}] to [{}]", amount, userId)
    Future.successful(
      PaymentResponse(message = "Transfer successful", amount = amount)
    )
  }

}
