package mappings

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import model.Category
import java.time.Instant

object CategoryMappings {

    import doobie.implicits.javatimedrivernative._

    given Read[Category] = Read[(Long, String, String, Instant, Instant)].map {
        case (id, name, color, createdAt, updatedAt) =>
            Category(id, name, color, createdAt, updatedAt)
    }
}