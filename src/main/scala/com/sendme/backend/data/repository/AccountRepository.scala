package com.sendme.backend.data.repository

import com.sendme.backend.data.entity.Account

import scala.concurrent.{ ExecutionContext, Future }

class AccountRepository(implicit ec: ExecutionContext) extends SlickBackend[UserRepository](tableName = "accounts") {

  import profile.api._

  final class UserAccountSchema(tag: Tag) extends DefaultSchema[Account](tag) {
    def userId  = column[Int]("user_id")
    def balance = column[Double]("balance")

    def * = (id.?, userId, balance, dateCreated) <> (Account.tupled, Account.unapply)
  }

  final class UserAccountTableOps extends TableOperation[Account, UserAccountSchema] {
    override val table = TableQuery[UserAccountSchema]

    override def create(entity: Account): Future[Account] = super.create(entity)

    def updateAmount(
      accountId: Int,
      amount: Double
    ): Future[Int] = findById(accountId).flatMap {
      case None          =>
        Future.failed(new Exception("The account does not exist"))
      case Some(account) =>
        val q = for { a <- table if a.userId === accountId } yield a.balance
        database.run(q.update(account.balance + amount))
    }

    def findByUser(userId: Int): Future[Seq[Account]] =
      database.run(table.filter(_.userId === userId).result)
  }
}

object AccountRepository {
  def apply()(implicit ec: ExecutionContext): AccountRepository = new AccountRepository()
}
