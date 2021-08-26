package com.sendme.backend.data.cache

import com.sendme.backend.UnitSpec
import com.sendme.backend.config.RedisConfig
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Seconds, Span }
import redis.embedded.RedisServer

import scala.concurrent.ExecutionContext

class RedisBackendSpec extends UnitSpec with ScalaFutures with BeforeAndAfterAll {
  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(Span(6, Seconds), Span(1, Seconds))

  private val redisConfig  = RedisConfig.getConfig(ConfigFactory.load())
  private val redisService =
    RedisServer
      .builder()
      .port(redisConfig.port)
      .setting(s"requirepass ${redisConfig.secret.getOrElse("")}")
      .build()

  private implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(
    new java.util.concurrent.ForkJoinPool(10)
  )

  "The RedisBackend" should {
    "behave correctly" in {
      val cache = RedisBackend(redisConfig)
      val key   = "a"
      val value = "b"

      withClue("when function to persist is called") {
        whenReady {
          cache.addElement(key, value, None)
        } { result => result shouldBe true }
      }

      withClue("when function to retrieve is called") {
        whenReady {
          cache.getElement(key)
        } { result => result shouldBe Some(value) }
      }

      withClue("when function to delete is called") {
        whenReady {
          cache.removeElement(key)
        } { result => result shouldBe Some(1) }
      }
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    redisService.start()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    redisService.stop()
  }
}
