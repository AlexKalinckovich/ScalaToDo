import java.util.UUID
import java.time.Instant

case class Todo(
                   id: UUID,
                   description: String,
                   completed: Boolean,
                   createdAt: Instant,
                   updatedAt: Instant
               )