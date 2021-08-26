package com.sendme.backend.data.repository

import com.sendme.backend.data.entity.Payment

import scala.concurrent.{ ExecutionContext, Future }

class PaymentRepository(
  implicit ec: ExecutionContext
) extends SlickBackend[UserRepository](tableName = "payments") {

  import profile.api._

  final class PaymentSchema(tag: Tag) extends DefaultSchema[Payment](tag) {
    def identifier = column[String]("identifier")
    def provider   = column[String]("provider")
    def amount     = column[Double]("amount")
    def accountId  = column[Int]("account_id")

    def * = (identifier, provider, amount, accountId, dateCreated) <> (Payment.tupled, Payment.unapply)
  }

  final class PaymentTableOps extends TableOperation[Payment, PaymentSchema] {
    override val table = TableQuery[PaymentSchema]

    override def create(entity: Payment): Future[Payment] = super.create(entity)
  }
}

object PaymentRepository {
  def apply()(implicit ec: ExecutionContext) = new PaymentRepository()
}
