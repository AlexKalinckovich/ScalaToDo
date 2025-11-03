import cats.effect.IO
import doobie.{Meta, Transactor}
import doobie.implicits.*
import doobie.postgres.implicits.*

import java.time.{Instant, LocalDate}
import java.util.UUID

implicit val importanceMeta: Meta[Importance] =
    pgEnumString("importance_type", Importance.valueOf, _.toString)


trait TodoRepository {
    def findAll(): IO[List[Todo]]
    def findById(id: UUID): IO[Option[Todo]]
    def create(request: CreateTodoRequest): IO[Todo] 
    def update(id: UUID, request: UpdateTodoRequest): IO[Option[Todo]]
    def delete(id: UUID): IO[Boolean]
}

class TodoRepositoryPostgres(xa: Transactor[IO]) extends TodoRepository {

    private val allColumns = fr"SELECT id, description, completed, created_at, updated_at, importance, deadline"

    override def findAll(): IO[List[Todo]] = {
        (allColumns ++ fr"FROM todos")
            .query[Todo]
            .to[List]
            .transact(xa)
    }

    override def findById(id: UUID): IO[Option[Todo]] = {
        (allColumns ++ fr"FROM todos WHERE id = $id")
            .query[Todo]
            .option
            .transact(xa)
    }

    override def create(request: CreateTodoRequest): IO[Todo] = {
        val id = UUID.randomUUID()
        val now = Instant.now()

        sql"""
          INSERT INTO todos (id, description, completed, created_at, updated_at, importance, deadline)
          VALUES (
            $id, 
            ${request.description}, 
            false, 
            $now, 
            $now, 
            ${request.importance}, 
            ${request.deadline}
          )
        """.update
            .withUniqueGeneratedKeys[Todo](
                "id", "description", "completed", "created_at", "updated_at", "importance", "deadline"
            )
            .transact(xa)
    }

    override def update(id: UUID, request: UpdateTodoRequest): IO[Option[Todo]] = {
        val now = Instant.now()

        sql"""
          UPDATE todos
          SET 
              description = ${request.description}, 
              completed = ${request.completed}, 
              importance = ${request.importance},
              deadline = ${request.deadline},
              updated_at = $now
          WHERE id = $id
        """.update.run
            .transact(xa)
            .flatMap { rowsUpdated =>
                if (rowsUpdated > 0) {
                    findById(id) 
                } else {
                    IO.pure(None) 
                }
            }
    }

    override def delete(id: UUID): IO[Boolean] = {
        sql"DELETE FROM todos WHERE id = $id"
            .update.run
            .transact(xa)
            .map(_ > 0) 
    }
}

object TodoRepositoryPostgres {
    def apply(xa: Transactor[IO]): TodoRepositoryPostgres = new TodoRepositoryPostgres(xa)
}