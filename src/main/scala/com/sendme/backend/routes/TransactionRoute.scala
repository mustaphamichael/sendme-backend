package com.sendme.backend.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.sendme.backend.data.Transaction
import com.sendme.backend.routes.TransactionRoute.{ OutgoingPayload, SendMoneyPayload }
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

  val route: Route = pathPrefix("transaction") { sendMoney }

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
                  SuccessResponse(data = OutgoingPayload.sendMoney(req.receiver_id, transaction))
                )
            }
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

  final case class SendMoneyResponse(
    receiver_id: Int,
    transaction_code: String,
    transaction_amount: Double,
    transaction_status: String,
    transaction_description: String,
    transaction_date: Instant
  ) extends OutgoingPayload

  object OutgoingPayload {
    def sendMoney(
      receiverId: Int,
      transaction: Transaction
    ): SendMoneyResponse = SendMoneyResponse(
      receiver_id             = receiverId,
      transaction_code        = transaction.code,
      transaction_amount      = transaction.amount,
      transaction_status      = transaction.status,
      transaction_description = transaction.description,
      transaction_date        = transaction.dateCreated
    )
  }

  def apply(
    transactionService: TransactionService
  )(implicit ec: ExecutionContext) = new TransactionRoute(transactionService)
}
