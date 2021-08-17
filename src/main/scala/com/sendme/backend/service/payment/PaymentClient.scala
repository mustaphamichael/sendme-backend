package com.sendme.backend.service.payment

import akka.http.scaladsl.model.StatusCodes
import com.sendme.backend.data.{ Account, Payment }
import com.sendme.backend.data.repository.PaymentRepository
import com.sendme.backend.service.payment.PaymentProfile.{ PaymentRequest, PaymentResponse }
import com.sendme.backend.util.ApiException
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

class PaymentClient(
  config: Config,
  paymentRepository: PaymentRepository
)(implicit ec: ExecutionContext) {

  private val log         = LoggerFactory.getLogger(getClass.getName)
  private val paymentRepo = new paymentRepository.PaymentTableOps

  private val paymentProvider = config.getString("provider.payment.enabled").toLowerCase match {
    case "demo" => PaymentProfile.DemoProfile
    case _      =>
      log.warn("Could not find the specified provider, defaulting to DEMO provider")
      PaymentProfile.DemoProfile
  }

  def receivePayment(
    amount: Double,
    account: Account
  ): Future[Payment] = {
    paymentProvider
      .client(config)
      .receivePayment(
        payload = PaymentRequest(account, amount)
      )
      .recoverWith { case NonFatal(exception) =>
        log.error("Receiving payment from [{}] with [{}] failed with [{}] ", account.userId, paymentProvider.name, exception)
        Future.failed(ApiException(StatusCodes.InternalServerError, "Cannot receive payment at this moment"))
      }
      .flatMap { response =>
        saveRecord(response, account)
      }
  }

  def makeTransfer(
    amount: Double,
    account: Account
  ): Future[Payment] = {
    paymentProvider
      .client(config)
      .makeTransfer(
        payload = PaymentRequest(account, amount)
      )
      .recoverWith { case NonFatal(exception) =>
        log.error("Making transfer to [{}] with [{}] failed with [{}] ", account.userId, paymentProvider.name, exception)
        Future.failed(ApiException(StatusCodes.InternalServerError, "Cannot receive payment at this moment"))
      }
      .flatMap { response =>
        saveRecord(response, account)
      }
  }

  private def saveRecord(response: PaymentResponse, account: Account): Future[Payment] = {
    val payment = Payment(
      identifier  = response.reference,
      provider    = paymentProvider.name,
      amount      = response.amount,
      accountId   = account.id.getOrElse(0),
      dateCreated = response.created
    )
    paymentRepo.create(payment)
  }
}

object PaymentClient {
  def apply(
    config: Config,
    paymentRepository: PaymentRepository
  )(implicit ec: ExecutionContext) = new PaymentClient(config, paymentRepository)
}
