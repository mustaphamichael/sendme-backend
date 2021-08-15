package com.sendme.backend.util

import akka.http.scaladsl.model.StatusCode

case class ApiException(
  status: StatusCode,
  message: String
) extends Exception
