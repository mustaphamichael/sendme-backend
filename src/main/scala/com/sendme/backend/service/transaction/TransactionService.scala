package com.sendme.backend.service.transaction

import akka.http.scaladsl.model.StatusCodes._
import com.sendme.backend.data.entity.{ Account, AddMoney, Failed, SendMoney, Successful, Transaction, User }
import com.sendme.backend.data.repository.{ AccountRepository, TransactionRepository, UserRepository }
import com.sendme.backend.service.email.EmailClient
import com.sendme.backend.service.payment.PaymentClient
import com.sendme.backend.util.ApiException

import scala.concurrent.{ ExecutionContext, Future }

class TransactionService(
  userRepository: UserRepository,
  accountRepository: AccountRepository,
  transactionRepository: TransactionRepository,
  paymentClient: PaymentClient,
  emailClient: EmailClient
)(implicit ec: ExecutionContext) {

  private val userRepo        = new userRepository.UserTableOps
  private val accountRepo     = new accountRepository.UserAccountTableOps
  private val transactionRepo = new transactionRepository.TransactionTableOps

  /** Send money to another user
    * 1- Confirm that the user initiating the transaction is the owner of the account
    * 2. Check that the account has enough funds to proceed
    */
  def sendMoney(senderId: Int, senderAccountId: Int, receiverAccountId: Int, amount: Double): Future[Transaction] = {
    if (amount < 100.0) Future.failed(ApiException(BadRequest, "Amount should not be lower than 100.00"))
    else {
      (for {
        senderInfo <- getUserInfoAndAccount(senderAccountId)
        receiverInfo <- getUserInfoAndAccount(receiverAccountId)
      } yield (senderInfo, receiverInfo)).flatMap {
        case (
              (sender, senderAccount),
              (receiver, receiverAccount)
            ) =>
          if (sender.id.get != senderId)
            Future.failed(
              ApiException(
                Unauthorized,
                s"You do not have the permission to carry out this transaction, because account [$senderAccountId] does not belong to you"
              )
            )
          else if (senderAccount.balance < amount) Future.failed(ApiException(BadRequest, "Insufficient funds"))
          else
            sendMoneyTransaction(senderAccountId, receiverAccountId, amount).flatMap { success =>
              val senderTx = Transaction.create(
                account         = senderAccount,
                amount          = amount,
                transactionType = SendMoney,
                status          = Successful,
                description     = s"Send money to ${receiver.name}-${receiver.id}"
              )
              if (success) {
                val receiverTx = Transaction.create(
                  account         = receiverAccount,
                  amount          = amount,
                  transactionType = AddMoney,
                  status          = Successful,
                  description     = s"Received money from ${sender.name}-${sender.id}"
                )

                (for {
                  s <- transactionRepo.create(senderTx)
                  _ <- transactionRepo.create(receiverTx)
                } yield s).flatMap { transaction =>
                  emailClient.sendMail(
                    to      = sender.email,
                    subject = s"SendMe Transaction #${transaction.code}",
                    content = s"Your request to send NGN $amount to ${receiver.name} was ${transaction.status}"
                  )

                  emailClient.sendMail(
                    to      = receiver.email,
                    subject = s"SendMe Transaction #${transaction.code}",
                    content = s"You have just received NGN $amount from ${sender.name}"
                  )
                  Future.successful(transaction)
                }
              }
              else {
                transactionRepo.create(senderTx.copy(status = Failed.toString)).flatMap { _ =>
                  Future.failed(new Exception(s"Error creating failed transaction for ${sender.id}"))
                }
              }
            }
      }
    }
  }

  private def getUserInfoAndAccount(accountId: Int): Future[(User, Account)] =
    (for {
      account <- accountRepo.findById(accountId)
      user <- userRepo.findById(account.get.userId)
    } yield (user, account))
      .recoverWith { _ => Future.failed(ApiException(NotFound, s"The account [$accountId] does not exist")) }
      .flatMap { case (Some(user), Some(account)) =>
        Future.successful(user -> account)
      }

  private def sendMoneyTransaction(senderId: Int, receiverId: Int, amount: Double): Future[Boolean] =
    for {
      decrement <- accountRepo.updateAmount(senderId, -amount)
      increment <- accountRepo.updateAmount(receiverId, amount)
    } yield decrement + increment >= 2

  /** Top up user account */
  def addMoney(amount: Double, accountId: Int): Future[Transaction] = {
    if (amount < 100.0) Future.failed(ApiException(BadRequest, "Amount should not be lower than 100.00"))
    else {
      getUserInfoAndAccount(accountId).flatMap { case (user, account) =>
        paymentClient.receivePayment(amount, account).flatMap { _ =>
          val transaction = Transaction.create(
            account         = account,
            amount          = amount,
            transactionType = AddMoney,
            status          = Successful,
            description     = "Added money to account"
          )

          for {
            _ <- accountRepo.updateAmount(accountId, amount)
            transaction <- transactionRepo.create(transaction)
          } yield {
            emailClient.sendMail(
              to      = user.email,
              subject = s"SendMe Transaction #${transaction.code}",
              content = s"Your request to top up your account with NGN $amount was ${transaction.status}.\n\n"
            )
            transaction
          }
        }
      }
    }
  }

  def fetchTransactions(userId: Int, limit: Option[Int], page: Option[Int]): Future[Seq[Transaction]] =
    transactionRepo.allByUser(userId, limit, page)

  def fetchTransactionsByAccount(accountId: Int, limit: Option[Int], page: Option[Int]): Future[Seq[Transaction]] =
    transactionRepo.allByAccount(accountId, limit, page)

  def fetchTransactionDetails(code: String): Future[Option[Transaction]] =
    transactionRepo.findByCode(code)
}

object TransactionService {
  def apply(
    userRepository: UserRepository,
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    paymentClient: PaymentClient,
    emailClient: EmailClient
  )(implicit ec: ExecutionContext) =
    new TransactionService(userRepository, accountRepository, transactionRepository, paymentClient, emailClient)
}
