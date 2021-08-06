package com.sendme.backend.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HashingUtilTest extends AnyWordSpec with Matchers {
  "The Hashing utility" should {
    "hash and verify secret correctly" in {
      for {
        hashed <- HashingUtil.hashPassword("pass")
        isValid <- HashingUtil.verifyHash("pass", hashed)
      } assert(isValid === true)
    }
  }
}
