package com.sendme.backend.service.payment

import com.sendme.backend.data.Account
import com.sendme.backend.service.payment.providers.DemoProvider
import com.typesafe.config.Config

import java.time.Instant
import java.util.UUID

trait PaymentProfile[T] {
  def name: String

  def client(
    config: Config
  ): T

}

object PaymentProfile {
  final case class PaymentRequest(
    account: Account,
    amount: Double,
    created: Instant = Instant.now()
  )

  final case class PaymentResponse(
    amount: Double,
    message: String,
    reference: String = UUID.randomUUID().toString,
    created: Instant  = Instant.now()
  )

  case object DemoProfile extends PaymentProfile[DemoProvider] {
    override val name: String = "Demo"

    override def client(config: Config): DemoProvider = new DemoProvider
  }
}
