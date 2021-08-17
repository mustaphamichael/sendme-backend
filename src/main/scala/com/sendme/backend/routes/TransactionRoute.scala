package com.sendme.backend.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.sendme.backend.data.Transaction
import com.sendme.backend.routes.TransactionRoute.{ AddMoneyPayload, OutgoingPayload, SendMoneyPayload }
import com.sendme.backend.service.transaction.TransactionService
import com.sendme.backend.util.{ ApiException, FailureResponse, SuccessResponse }
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.slf4j.LoggerFactory

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class TransactionRoute(
  transactionService: TransactionService
)(implicit ec: ExecutionContext) {
  private val log = LoggerFactory.getLogger(getClass.getName)

  val route: Route = pathPrefix("transactions") { sendMoney ~ addMoney }

  private def sendMoney: Route =
    path("send-money") {
      post {
        headerValueByName("id") { userId =>
          entity(as[SendMoneyPayload]) { req =>
            log.info("Received request to send money from [{}] to [{}]", userId, req.receiver_id)

            onComplete(
              transactionService.sendMoney(userId.toInt, req.receiver_id, req.amount)
            ) {
              case Failure(ApiException(status, message)) =>
                log.warn("Sending money failed with [{}]", message)
                complete(status, FailureResponse(message))
              case Failure(exception)                     =>
                log.warn("Sending money failed with [{}]", exception)
                complete(StatusCodes.InternalServerError)
              case Success(transaction)                   =>
                complete(
                  SuccessResponse(data = OutgoingPayload.sendMoney(transaction))
                )
            }
          }
        }
      }
    }

  private def addMoney(): Route =
    path("add-money") {
      post {
        entity(as[AddMoneyPayload]) { req =>
          log.info("Received request to add money to account [{}]", req.account_id)

          onComplete(
            transactionService.addMoney(req.amount, req.account_id)
          ) {
            case Failure(ApiException(status, message)) =>
              log.warn("Adding money into account [{}] failed with [{}]", req.account_id, message)
              complete(status, FailureResponse(message))
            case Failure(exception)                     =>
              log.warn("Adding money into account [{}] failed with [{}]", req.account_id, exception)
              complete(StatusCodes.InternalServerError)
            case Success(transaction)                   =>
              complete(SuccessResponse(data = OutgoingPayload.addMoney(transaction)))
          }
        }
      }
    }

}

object TransactionRoute {
  trait IncomingPayload
  trait OutgoingPayload

  final case class SendMoneyPayload(
    amount: Double,
    receiver_id: Int
  ) extends IncomingPayload

  final case class AddMoneyPayload(
    amount: Double,
    account_id: Int
  ) extends IncomingPayload

  final case class SendMoneyResponse(
    transaction_code: String,
    transaction_amount: Double,
    transaction_status: String,
    transaction_date: Instant
  ) extends OutgoingPayload

  final case class AddMoneyResponse(
    transaction_code: String,
    transaction_amount: Double,
    transaction_status: String,
    transaction_date: Instant
  ) extends OutgoingPayload

  object OutgoingPayload {
    def sendMoney(
      transaction: Transaction
    ): SendMoneyResponse = SendMoneyResponse(
      transaction_code   = transaction.code,
      transaction_amount = transaction.amount,
      transaction_status = transaction.status,
      transaction_date   = transaction.dateCreated
    )

    def addMoney(
      transaction: Transaction
    ): AddMoneyResponse = AddMoneyResponse(
      transaction_code   = transaction.code,
      transaction_amount = transaction.amount,
      transaction_status = transaction.status,
      transaction_date   = transaction.dateCreated
    )
  }

  def apply(
    transactionService: TransactionService
  )(implicit ec: ExecutionContext) = new TransactionRoute(transactionService)
}
