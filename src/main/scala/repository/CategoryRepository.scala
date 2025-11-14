package repository

import cats.effect.IO
import cats.implicits._
import doobie.implicits.*
import doobie.{Transactor, Fragments}
import doobie.postgres.implicits.*
import model.{Category, CategoryCreateRequest, CategoryUpdateRequest, CategoryPatchRequest}
import mappings.CategoryMappings.given
import mappings.DoobieMappings.given

import java.time.Instant

trait CategoryRepository {
    def findAll(): IO[List[Category]]
    def findById(id: Long): IO[Option[Category]]
    def findByIds(ids: List[Long]): IO[List[Category]]
    def create(request: CategoryCreateRequest): IO[Category]
    def update(category: Category): IO[Option[Category]]
    def delete(id: Long): IO[Boolean]
}

class CategoryRepositoryPostgres(xa: Transactor[IO]) extends CategoryRepository {

    private val baseColumns = fr"SELECT id, name, color, created_at, updated_at"

    override def findAll(): IO[List[Category]] = {
        (baseColumns ++ fr"FROM categories")
            .query[Category]
            .to[List]
            .transact(xa)
    }

    override def findById(id: Long): IO[Option[Category]] = {
        (baseColumns ++ fr"FROM categories WHERE id = $id")
            .query[Category]
            .option
            .transact(xa)
    }

    override def findByIds(ids: List[Long]): IO[List[Category]] = {
        if (ids.isEmpty) IO.pure(List.empty)
        else {
            (baseColumns ++ fr"FROM categories WHERE id IN ($ids)")
                .query[Category]
                .to[List]
                .transact(xa)
        }
    }

    override def create(request: CategoryCreateRequest): IO[Category] = {
        val now = Instant.now()
        sql"""
          INSERT INTO categories (name, color, created_at, updated_at)
          VALUES (${request.name}, ${request.color}, $now, $now)
        """.update
            .withUniqueGeneratedKeys[Long]("id")
            .transact(xa)
            .flatMap(id => findById(id).map(_.get))
    }

    override def update(category: Category): IO[Option[Category]] = {
        val now = Instant.now()
        sql"""
          UPDATE categories
          SET 
              name = ${category.name}, 
              color = ${category.color},
              updated_at = $now
          WHERE id = ${category.id}
        """.update.run
            .transact(xa)
            .flatMap { rowsUpdated =>
                if (rowsUpdated > 0) {
                    findById(category.id)
                } else {
                    IO.pure(None)
                }
            }
    }

    override def delete(id: Long): IO[Boolean] = {
        sql"DELETE FROM categories WHERE id = $id"
            .update.run
            .transact(xa)
            .map(_ > 0)
    }
}

object CategoryRepositoryPostgres {
    def apply(xa: Transactor[IO]): CategoryRepositoryPostgres = new CategoryRepositoryPostgres(xa)
}