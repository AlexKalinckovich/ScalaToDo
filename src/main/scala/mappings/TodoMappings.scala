package mappings

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import model.{Todo, Category, Importance}
import java.time.{Instant, LocalDate}
import mappings.DoobieMappings.given_Meta_Importance
object TodoMappings {


    import doobie.implicits.javatimedrivernative._

    given Read[Todo] = Read[(Long, String, Boolean, Instant, Instant, Importance, Option[LocalDate], Option[Long])].map {
        case (id, desc, completed, createdAt, updatedAt, importance, deadline, categoryId) =>
            Todo(
                id = id,
                description = desc,
                completed = completed,
                createdAt = createdAt,
                updatedAt = updatedAt,
                importance = importance,
                deadline = deadline,
                categoryId = categoryId,
                category = None 
            )
    }
}