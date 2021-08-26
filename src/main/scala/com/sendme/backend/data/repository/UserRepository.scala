package com.sendme.backend.data.repository

import com.sendme.backend.data.entity.User
import com.sendme.backend.util.HashingUtil

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class UserRepository(implicit ec: ExecutionContext) extends SlickBackend[User](tableName = "users") {

  import profile.api._

  final class UserSchema(tag: Tag) extends DefaultSchema[User](tag) {
    def name     = column[String]("display_name")
    def email    = column[String]("email")
    def password = column[String]("password")

    def * = (id.?, name, email, password, dateCreated) <> ((User.apply _).tupled, User.unapply)
  }

  final class UserTableOps extends TableOperation[User, UserSchema] {
    override val table = TableQuery[UserSchema]

    // encrypt password and save to db
    // populate the user account with a balance of 0
    override def create(
      user: User
    ): Future[User] = findByEmail(user.email).flatMap {
      case None    =>
        super.create(user)
      case Some(_) =>
        Future.failed(new Exception("This user already exists"))
    }

    def authenticate(
      email: String,
      password: String
    ): Future[Option[User]] = findByEmail(email).flatMap {
      case None        =>
        Future.failed(new Exception("The user does not exist"))
      case Some(value) =>
        HashingUtil.verifyHash(password, value.password) match {
          case Failure(_)       =>
            Future.failed(new Exception("Password not valid"))
          case Success(isMatch) =>
            if (isMatch) Future.successful(Some(value))
            else Future.failed(new Exception("Password not valid"))
        }
    }

    def findByEmail(email: String): Future[Option[User]] =
      database.run(table.filter(_.email === email).result.headOption)
  }

}

object UserRepository {
  def apply()(implicit ec: ExecutionContext): UserRepository = new UserRepository()
}
