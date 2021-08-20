package com.sendme.backend.data.repository

import com.sendme.backend.config.SlickConfig
import com.sendme.backend.data.DefaultEntity
import com.typesafe.config.ConfigFactory
import slick.jdbc.JdbcBackend
import slick.lifted

import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }

class SlickBackend[E](
  protected val tableName: String
)(implicit ec: ExecutionContext) {

  val profile        = slick.jdbc.PostgresProfile
  private val config = SlickConfig.getConfig(ConfigFactory.load())

  val database = JdbcBackend.Database.forURL(
    url                 = config.url,
    user                = config.username,
    password            = config.password,
    driver              = config.driver,
    keepAliveConnection = config.keepAlive
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

    /* Fetch all records with offset and limit */
    def all(_limit: Option[Int], _page: Option[Int])(f: S => Rep[Boolean]): Future[Seq[D]] = {
      val limit = _limit.getOrElse(10)
      val page  = _page.getOrElse(1)
      database.run(
        table
          .filter(f)
          .sortBy(_.dateCreated.desc)
          .drop(limit * (page - 1).abs)
          .take(limit)
          .result
      )
    }
  }
}

object SlickBackend {
  def apply[E](
    tableName: String
  )(implicit ec: ExecutionContext): SlickBackend[E] = new SlickBackend[E](tableName)
}
