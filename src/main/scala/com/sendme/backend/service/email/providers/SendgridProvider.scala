package com.sendme.backend.service.email.providers

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpMethods, HttpRequest, StatusCodes }
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.sendme.backend.service.email.EmailProfile.Email
import com.sendme.backend.service.email.providers.SendgridProvider.SendgridConfig
import com.sendme.backend.service.email.{ EmailProvider, EmailProviderConfig }
import com.typesafe.config.Config
import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

/** See Sendgrid documentation [[https://docs.sendgrid.com/for-developers/sending-email/curl-examples]] for more details */
protected[service] class SendgridProvider(
  config: SendgridConfig
)(implicit actorSystem: ActorSystem[Nothing])
    extends EmailProvider[Email, String] {
  import SendgridProvider._

  private val jsonWriter: Printer = Printer.noSpaces.copy(dropNullValues = false)

  private val log = LoggerFactory.getLogger(getClass.getName)

  private implicit val executionContext: ExecutionContext = actorSystem.executionContext

  override def name: String = "sendgrid-email-provider"

  override def sendMessage(email: Email): Future[String] = {
    log.info("Received request to send email [{}]", email)
    Http()
      .singleRequest(
        HttpRequest(
          method  = HttpMethods.POST,
          uri     = config.url,
          headers = List(RawHeader("Authorization", s"Bearer ${config.apiKey}")),
          entity  = HttpEntity(
            ContentTypes.`application/json`,
            jsonWriter.print(
              EmailPayload(
                from             = Address(email.from, config.sender),
                personalizations = Seq(Recipient(to = Seq(Address(email.to)))),
                subject          = email.subject,
                content          = Seq(Text(email.content))
              ).asJson
            )
          )
        )
      )
      .recoverWith { case NonFatal(exception) =>
        log.error(
          "Exception encountered while making request to [{}]: [{} - {}]",
          config.url,
          exception.getClass.getSimpleName
        )
        Future.failed(exception)
      }
      .flatMap { response =>
        response.status match {
          case StatusCodes.OK | StatusCodes.Accepted =>
            log.info("Request to [{}] successful with statusCode [{}]", config.url, response.status)
            Unmarshal(response).to[String]

          case _ =>
            log.error("Request to endpoint [{}] failed with statusCode [{}]", config.url, response.status)
            Future.failed(new Exception(s"Sending email failed with status code [${response.status}]"))
        }
      }
  }
}

object SendgridProvider {
  final case class EmailPayload(
    personalizations: Seq[Recipient],
    from: Address,
    subject: String,
    content: Seq[Text]
  )
  final case class Address(email: String, name: String = "")
  final case class Recipient(to: Seq[Address])
  final case class Text(value: String, `type`: String = "text/plain")

  final case class SendgridConfig(
    url: String,
    apiKey: String,
    sender: String
  ) extends EmailProviderConfig

  object SendgridConfig {
    def fromConfig(config: Config): SendgridConfig = SendgridConfig(
      url    = config.getString("messaging.email.sendgrid.url"),
      apiKey = config.getString("messaging.email.sendgrid.api-key"),
      sender = config.getString("messaging.sender-name")
    )
  }

  def apply(
    config: Config
  )(implicit actorSystem: ActorSystem[Nothing]) = new SendgridProvider(SendgridConfig.fromConfig(config))
}
