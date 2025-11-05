package error

import cats.data.NonEmptyList

import java.util.UUID

sealed trait AppError extends Throwable

case class ValidationErrors(messages: NonEmptyList[String]) extends AppError {
    override def getMessage: String = messages.toList.mkString(", ")
}

case class TodoNotFound(id: UUID) extends AppError {
    override def getMessage: String = s"Todo with id $id not found"
}

case class InvalidUuid(raw: String) extends AppError {
    override def getMessage: String = s"Invalid UUID: $raw"
}