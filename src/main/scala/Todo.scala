import java.time.{Instant, LocalDate}
import java.util.UUID

enum Importance {
    case High, Medium, Low
}

case class Todo(
                   id: UUID,
                   description: String,
                   completed: Boolean,
                   createdAt: Instant,
                   updatedAt: Instant,
                   importance: Importance,  
                   deadline: Option[LocalDate] 
               )

case class CreateTodoRequest(
                                description: String,
                                importance: Importance,
                                deadline: Option[LocalDate]
                            )

case class UpdateTodoRequest(
                                description: String,
                                completed: Boolean,
                                importance: Importance,
                                deadline: Option[LocalDate]
                            )

case class TodoResponse(
                           id: UUID,
                           description: String,
                           completed: Boolean,
                           createdAt: Instant,
                           updatedAt: Instant,
                           importance: Importance,
                           deadline: Option[LocalDate]
                       )