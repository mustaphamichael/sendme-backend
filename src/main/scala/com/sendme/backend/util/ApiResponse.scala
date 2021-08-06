package com.sendme.backend.util

sealed trait ApiResponse

case class SuccessResponse[T](
  data: T
) extends ApiResponse

case class MessagePayload(
  message: String = "Successful"
) extends ApiResponse

case class FailureResponse(reason: String) extends ApiResponse
