package error

import cats.data.NonEmptyList

sealed trait AppError extends Throwable

case class ValidationErrors(messages: NonEmptyList[String]) extends AppError {
    override def getMessage: String = messages.toList.mkString(", ")
}

case class InvalidId(id: String) extends AppError {
    override def getMessage: String = s"Invalid ID: $id"
}

case class TodoNotFound(id: Long) extends AppError {
    override def getMessage: String = s"Todo with id $id not found"
}

case class CategoryNotFound(id: Long) extends AppError {
    override def getMessage: String = s"Category with id $id not found"
}