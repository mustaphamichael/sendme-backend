package com.sendme.backend.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class JwtGeneratorSpec extends AnyWordSpec with Matchers {

  "The JwtGenerator" should {
    "generate and decode jwt token " in {
      val token = JwtGenerator.generateToken(email, audience)
      for {
        claim <- JwtGenerator.decodeToken(token)
      } {
        assert(claim.content === s"""{"email":"$email"}""")
        assert(claim.subject === Some(subject))
      }
    }
  }

  private val email    = "mm@email.com"
  private val audience = "users"
  private val subject  = "authentication"
}
