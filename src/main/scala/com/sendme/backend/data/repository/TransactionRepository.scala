package com.sendme.backend.data.repository

import com.sendme.backend.data.Transaction

import scala.concurrent.{ ExecutionContext, Future }

class TransactionRepository(implicit ec: ExecutionContext)
    extends SlickBackend[UserRepository](
      tableName = "transactions"
    ) {

  import profile.api._

  final class TransactionSchema(tag: Tag) extends DefaultSchema[Transaction](tag) {
    def accountId              = column[Int]("account_id")
    def transactionCode        = column[String]("transaction_code")
    def transactionAmount      = column[Double]("transaction_amount")
    def transactionType        = column[String]("transaction_type")
    def transactionStatus      = column[String]("transaction_status")
    def transactionDescription = column[String]("transaction_description")

    def * = (
      accountId,
      transactionCode,
      transactionAmount,
      transactionType,
      transactionStatus,
      transactionDescription,
      dateCreated
    ) <> ((Transaction.apply _).tupled, Transaction.unapply)
  }

  final class TransactionTableOps extends TableOperation[Transaction, TransactionSchema] {
    override val table = TableQuery[TransactionSchema]

    override def create(entity: Transaction): Future[Transaction] = super.create(entity)

    def findByCode(code: String): Future[Option[Transaction]] =
      database.run(table.filter(_.transactionCode === code).result.headOption)
  }
}

object TransactionRepository {
  def apply()(implicit ec: ExecutionContext) = new TransactionRepository()
}
