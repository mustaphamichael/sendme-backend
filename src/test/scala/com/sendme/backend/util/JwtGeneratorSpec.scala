package com.sendme.backend.util

import com.sendme.backend.config.JwtConfig
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class JwtGeneratorSpec extends AnyWordSpec with Matchers {

  "The JwtGenerator" should {
    "generate and decode jwt token " in {
      val config    = JwtConfig.fromConfig(ConfigFactory.load())
      val generator = JwtGenerator(config)
      val token     = generator.generateToken(id, audience)
      for {
        claim <- generator.decodeToken(token)
      } {
        assert(claim.content === s"""{"id":"$id"}""")
        assert(claim.audience === Some(Set(audience)))
      }
    }
  }

  private val id       = "1"
  private val audience = "users"
}
