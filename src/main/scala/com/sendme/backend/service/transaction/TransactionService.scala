package com.sendme.backend.service.transaction

import akka.http.scaladsl.model.StatusCodes._
import com.sendme.backend.data.{ Account, AddMoney, Failed, SendMoney, Successful, Transaction, User }
import com.sendme.backend.data.repository.{ AccountRepository, TransactionRepository, UserRepository }
import com.sendme.backend.util.ApiException

import scala.concurrent.{ ExecutionContext, Future }

class TransactionService(
  userRepository: UserRepository,
  accountRepository: AccountRepository,
  transactionRepository: TransactionRepository
)(implicit ec: ExecutionContext) {

  private val userRepo        = new userRepository.UserTableOps
  private val accountRepo     = new accountRepository.UserAccountTableOps
  private val transactionRepo = new transactionRepository.TransactionTableOps

  /** Send money to another user */
  def sendMoney(senderId: Int, receiverId: Int, amount: Double): Future[Transaction] = {
    if (amount < 100.0) Future.failed(ApiException(BadRequest, "Amount should not be lower than 100.00"))
    else {
      (for {
        sender <- getUserInfoAndAccount(senderId)
        receiver <- getUserInfoAndAccount(receiverId)
      } yield (sender, receiver)).flatMap {
        case (
              (sender, senderAccount),
              (receiver, receiverAccount)
            ) =>
          if (senderAccount.balance < amount) Future.failed(ApiException(BadRequest, "Insufficient funds"))
          else
            sendMoneyTransaction(senderId, receiverId, amount).flatMap { success =>
              val senderTx = Transaction.create(
                accountId       = senderAccount.id.get,
                amount          = amount,
                transactionType = SendMoney,
                status          = Successful,
                description     = s"Send money to ${receiver.name}-$receiverId"
              )
              if (success) {
                val receiverTx = Transaction.create(
                  accountId       = receiverAccount.id.get,
                  amount          = amount,
                  transactionType = AddMoney,
                  status          = Successful,
                  description     = s"Received money from ${sender.name}-$senderId"
                )

                (for {
                  s <- transactionRepo.create(senderTx)
                  _ <- transactionRepo.create(receiverTx)
                } yield s).flatMap { transaction =>
                  Future.successful(transaction)
                }
              }
              else {
                transactionRepo.create(senderTx.copy(status = Failed.toString)).flatMap { _ =>
                  Future.failed(new Exception(s"Error creating failed transaction for $senderId"))
                }
              }
            }
      }
    }
  }

  private def getUserInfoAndAccount(userId: Int): Future[(User, Account)] =
    (for {
      user <- userRepo.findById(userId)
      account <- accountRepo.findByUser(userId)
    } yield (user, account)).flatMap {
      case (None, None)                => Future.failed(ApiException(NotFound, "Invalid account"))
      case (Some(user), Some(account)) => Future.successful(user -> account)
    }

  private def sendMoneyTransaction(senderId: Int, receiverId: Int, amount: Double): Future[Boolean] =
    for {
      decrement <- accountRepo.updateAmount(senderId, -amount)
      increment <- accountRepo.updateAmount(receiverId, amount)
    } yield decrement + increment >= 2

  /** Top up user account */
  def addMoney(user: User, amount: Double): Future[Transaction] = ???
}

object TransactionService {
  def apply(
    userRepository: UserRepository,
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository
  )(implicit ec: ExecutionContext) =
    new TransactionService(userRepository, accountRepository, transactionRepository)
}
