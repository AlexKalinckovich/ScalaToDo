package model

import java.time.{Instant, LocalDate}
import java.util.UUID

enum Importance {
    case High, Medium, Low
}

case class Todo(
                   id:          Long,
                   description: String,
                   completed:   Boolean,
                   createdAt:   Instant,
                   updatedAt:   Instant,
                   importance:  Importance,
                   deadline:    Option[LocalDate],
                   categoryId:  Option[Long],
                   category:    Option[Category] = None
               )

case class CreateTodoRequest(
                                description: String,
                                importance: Importance,
                                deadline: Option[LocalDate],
                                categoryId: Option[Long]
                            )

case class PatchTodoRequest(
                               description: Option[String],
                               completed: Option[Boolean],
                               importance: Option[Importance],
                               deadline: Option[LocalDate],
                               categoryId: Option[Long]
                           )

case class UpdateTodoRequest(
                                description: String,
                                completed: Boolean,
                                importance: Importance,
                                deadline: Option[LocalDate],
                                categoryId: Option[Long]
                            )

case class TodoResponse(
                           id: Long,
                           description: String,
                           completed: Boolean,
                           createdAt: Instant,
                           updatedAt: Instant,
                           importance: Importance,
                           deadline: Option[LocalDate],
                           category: Option[Category]
                       )