package com.sendme.backend.service.email

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import com.sendme.backend.service.email.EmailProfile.Email
import com.sendme.backend.util.ApiException
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

class EmailClient(
  config: Config
)(implicit actorSystem: ActorSystem[Nothing]) {

  private val log = LoggerFactory.getLogger(getClass.getName)

  private implicit val ec: ExecutionContext = actorSystem.executionContext

  private val senderEmail = config.getString("messaging.sender-email")

  private val emailProvider = config.getString("messaging.email.provider").toLowerCase match {
    case "sendgrid" => EmailProfile.SendgridProfile
    case _          =>
      log.warn("Could not find the specified provider, defaulting to SENDGRID provider")
      EmailProfile.SendgridProfile
  }

  def sendMail(
    to: String,
    subject: String,
    content: String
  ): Future[String] = {
    emailProvider
      .client(config)
      .sendMessage(Email(senderEmail, to, subject, content))
      .recoverWith { case NonFatal(exception) =>
        log.error("Sending email to [{}] failed with [{}]", to, exception)
        Future.failed(ApiException(StatusCodes.InternalServerError, "Cannot send email at the moment"))
      }
  }
}

object EmailClient {
  def apply(config: Config)(implicit actorSystem: ActorSystem[Nothing]) = new EmailClient(config)
}
