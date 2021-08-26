package com.sendme.backend.service.email

import scala.concurrent.Future

trait EmailProvider[I, O] {
  def name: String

  def sendMessage(payload: I): Future[O]
}
