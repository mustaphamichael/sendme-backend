package com.sendme.backend.util

import com.github.t3hnar.bcrypt._

import scala.util.Try

object HashingUtil {
  def hashPassword(
    plainString: String
  ): Try[String] = plainString.bcryptSafe(10)

  def verifyHash(
    plainString: String,
    hashedPassword: String
  ): Try[Boolean] = plainString.isBcryptedSafe(hashedPassword)
}
