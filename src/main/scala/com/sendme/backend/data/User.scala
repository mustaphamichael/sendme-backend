package com.sendme.backend.data

import com.sendme.backend.routes.AuthRoute.SignUpPayload

import java.time.Instant

final case class User(
  override val id: Option[Int],
  name: String,
  email: String,
  password: String,
  dateCreated: Instant
) extends DefaultEntity

final case class Account(
  override val id: Option[Int],
  userId: Int,
  balance: Double,
  dateCreated: Instant
) extends DefaultEntity

final case class UserResponsePayload(
  id: Int,
  name: String,
  email: String
)

object User {
  def fromRequest(
    req: SignUpPayload
  ): User = User(
    id          = None,
    name        = req.name,
    email       = req.email,
    password    = req.password,
    dateCreated = Instant.now
  )

  def createAccount(
    user: User
  ): Account = Account(
    id          = None,
    userId      = user.id.getOrElse(0),
    balance     = 0,
    dateCreated = Instant.now
  )

  def response(
    user: User
  ): UserResponsePayload = UserResponsePayload(
    id    = user.id.getOrElse(0),
    name  = user.name,
    email = user.email
  )
}
