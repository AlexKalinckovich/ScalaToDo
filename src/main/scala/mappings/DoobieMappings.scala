package mappings

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import model.Importance

object DoobieMappings {
    given Meta[Importance] =
        pgEnumString("importance_type", Importance.valueOf, _.toString)
}