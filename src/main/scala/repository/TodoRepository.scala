package repository

import cats.effect.IO
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.{Meta, Transactor}
import model.{CreateTodoRequest, Importance, Todo, UpdateTodoRequest}
import mappings.DoobieMappings.given
import mappings.TodoMappings.given
import mappings.CategoryMappings.given

import java.time.Instant

trait TodoRepository {
    def findAll(): IO[List[Todo]]
    def findById(id: Long): IO[Option[Todo]]
    def create(request: CreateTodoRequest): IO[Todo]
    def update(todo: Todo): IO[Option[Todo]]
    def delete(id: Long): IO[Boolean]
}

class TodoRepositoryPostgres(xa: Transactor[IO]) extends TodoRepository {

    private val baseColumns = fr"SELECT t.id, t.description, t.completed, t.created_at, t.updated_at, t.importance, t.deadline, t.category_id"

    private val joinedColumns = fr"SELECT t.id, t.description, t.completed, t.created_at, t.updated_at, t.importance, t.deadline, t.category_id, c.id, c.name, c.color, c.created_at, c.updated_at"

    override def findAll(): IO[List[Todo]] = {
        (joinedColumns ++ fr"FROM todos t LEFT JOIN categories c ON t.category_id = c.id")
            .query[Todo]
            .to[List]
            .transact(xa)
    }

    override def findById(id: Long): IO[Option[Todo]] = {
        (joinedColumns ++ fr"FROM todos t LEFT JOIN categories c ON t.category_id = c.id WHERE t.id = $id")
            .query[Todo]
            .option
            .transact(xa)
    }

    override def create(request: CreateTodoRequest): IO[Todo] = {
        val now = Instant.now()
        sql"""
          INSERT INTO todos (description, completed, created_at, updated_at, importance, deadline, category_id)
          VALUES (
            ${request.description}, 
            false, 
            $now, 
            $now, 
            ${request.importance}, 
            ${request.deadline},
            ${request.categoryId}
          )
        """.update
            .withUniqueGeneratedKeys[Long]("id")
            .transact(xa)
            .flatMap(id => findById(id).map(_.get))
    }

    override def update(todo: Todo): IO[Option[Todo]] = {
        val now = Instant.now()
        sql"""
          UPDATE todos
          SET 
              description = ${todo.description}, 
              completed = ${todo.completed}, 
              importance = ${todo.importance},
              deadline = ${todo.deadline},
              category_id = ${todo.categoryId},
              updated_at = $now
          WHERE id = ${todo.id}
        """.update.run
            .transact(xa)
            .flatMap { rowsUpdated =>
                if (rowsUpdated > 0) {
                    findById(todo.id)
                } else {
                    IO.pure(None)
                }
            }
    }

    override def delete(id: Long): IO[Boolean] = {
        sql"DELETE FROM todos WHERE id = $id"
            .update.run
            .transact(xa)
            .map(_ > 0)
    }
}

object TodoRepositoryPostgres {
    def apply(xa: Transactor[IO]): TodoRepositoryPostgres = new TodoRepositoryPostgres(xa)
}