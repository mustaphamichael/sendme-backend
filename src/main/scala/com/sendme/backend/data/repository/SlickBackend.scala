package com.sendme.backend.data.repository

import com.sendme.backend.data.DefaultEntity
import com.typesafe.config.{ Config, ConfigFactory }
import slick.jdbc.JdbcBackend
import slick.lifted

import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }

class SlickBackend[E](
  protected val tableName: String
)(implicit ec: ExecutionContext) {

  val profile        = slick.jdbc.PostgresProfile
  val config: Config = ConfigFactory.load().getConfig("database")

  val database = JdbcBackend.Database.forURL(
    url                 = config.getString("url"),
    user                = config.getString("user"),
    password            = config.getString("password"),
    driver              = config.getString("driver"),
    keepAliveConnection = config.getBoolean("keep-alive-connection")
  )

  import profile.api._

  abstract class DefaultSchema[D <: DefaultEntity](tag: Tag) extends Table[D](tag, tableName) {
    def id          = column[Int]("id", O.AutoInc)
    def dateCreated = column[Instant]("created_at")
    def dateUpdated = column[Instant]("updated_at")
  }

  abstract class TableOperation[D <: DefaultEntity, S <: DefaultSchema[D]] {
    val table: lifted.TableQuery[S]

    def create(entity: D): Future[D] = database.run(
      table returning table += entity
    )

    def findById(id: Int): Future[Option[D]] =
      database.run(table.filter(_.id === id).result.headOption)
  }
}

object SlickBackend {
  def apply[E](
    tableName: String
  )(implicit ec: ExecutionContext): SlickBackend[E] = new SlickBackend[E](tableName)
}
