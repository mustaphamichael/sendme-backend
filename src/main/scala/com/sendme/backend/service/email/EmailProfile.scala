package com.sendme.backend.service.email

import akka.actor.typed.ActorSystem
import com.sendme.backend.service.email.providers.SendgridProvider
import com.typesafe.config.Config

private[email] trait EmailProviderConfig
trait EmailProfile[T] {
  def name: String

  def config(config: Config): EmailProviderConfig

  def client(config: Config)(implicit actorSystem: ActorSystem[Nothing]): T
}

object EmailProfile {

  case class Email(from: String, to: String, subject: String, content: String)

  case object SendgridProfile extends EmailProfile[SendgridProvider] {

    override val name: String = "Sendgrid"

    override def config(
      config: Config
    ): EmailProviderConfig = SendgridProvider.SendgridConfig.fromConfig(config)

    override def client(
      config: Config
    )(implicit actorSystem: ActorSystem[Nothing]): SendgridProvider = SendgridProvider(config)
  }
}
