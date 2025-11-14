package db

import cats.effect.IO
import cats.implicits.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.munit.TestContainerForAll
import db.Database
import db.migrations.Migrations
import doobie.implicits.*
import doobie.util.transactor.Transactor
import model.CategoryCreateRequest
import munit.CatsEffectSuite
import repository.CategoryRepositoryPostgres

import java.time.Instant
import scala.compiletime.uninitialized

class CategoryRepositoryPostgresSuite extends CatsEffectSuite with TestContainerForAll {

    override val containerDef: PostgreSQLContainer.Def =
        PostgreSQLContainer.Def(dockerImageName = "postgres:16-alpine")

    private var transactor: Transactor[IO] = uninitialized
    private var repository: CategoryRepositoryPostgres = uninitialized
    private var finalizer: IO[Unit] = IO.unit

    override def beforeAll(): Unit = {
        super.beforeAll()

        withContainers {
            case postgresContainer: PostgreSQLContainer =>
                val dbConfig = config.DbConfig(
                    url = postgresContainer.jdbcUrl,
                    user = postgresContainer.username,
                    pass = postgresContainer.password
                )

                val setup: IO[(Transactor[IO], CategoryRepositoryPostgres)] = for {
                    _ <- Migrations.run(dbConfig)
                    (xa, fin) <- Database.transactor(dbConfig).allocated
                    _ <- IO { finalizer = fin }
                } yield (xa, CategoryRepositoryPostgres(xa))

                val (xa, repo) = setup.unsafeRunSync()
                transactor = xa
                repository = repo
        }
    }

    override def afterAll(): Unit = {
        finalizer.unsafeRunSync()
        super.afterAll()
    }

    override def afterEach(context: AfterEach): Unit = {
        super.afterEach(context)
        if (transactor != null) {
            sql"DELETE FROM categories".update.run.transact(transactor).unsafeRunSync()
        }
    }

    test("findAll should return empty list when no categories exist") {
        repository.findAll().assertEquals(Nil)
    }

    test("create should insert a new category and return it with generated id") {
        val createRequest = CategoryCreateRequest(
            name = "Work",
            color = "#FF0000"
        )

        for {
            created <- repository.create(createRequest)
            all <- repository.findAll()
        } yield {
            assertEquals(created.name, "Work")
            assertEquals(created.color, "#FF0000")
            assert(created.id > 0)
            assert(created.createdAt != null)
            assert(created.updatedAt != null)
            assertEquals(all.length, 1)
            assertEquals(all.head.name, "Work")
        }
    }

    test("findById should return Some(category) when category exists") {
        val createRequest = CategoryCreateRequest("Find me", "#00FF00")

        for {
            created <- repository.create(createRequest)
            found <- repository.findById(created.id)
        } yield {
            assert(found.isDefined)
            assertEquals(found.get.id, created.id)
            assertEquals(found.get.name, "Find me")
            assertEquals(found.get.color, "#00FF00")
        }
    }

    test("findById should return None when category does not exist") {
        val nonExistentId = -1L
        repository.findById(nonExistentId).assertEquals(None)
    }

    test("findByIds should return categories for existing ids") {
        val categories = List(
            CategoryCreateRequest("Work", "#FF0000"),
            CategoryCreateRequest("Personal", "#00FF00"),
            CategoryCreateRequest("Shopping", "#0000FF")
        )

        for {
            created <- categories.traverse(repository.create)
            ids = created.map(_.id)
            found <- repository.findByIds(ids)
        } yield {
            assertEquals(found.length, 3)
            assertEquals(found.map(_.name).toSet, Set("Work", "Personal", "Shopping"))
        }
    }

    test("findByIds should return empty list for empty input") {
        repository.findByIds(List.empty).assertEquals(Nil)
    }

    test("findByIds should return only existing categories") {
        for {
            category <- repository.create(CategoryCreateRequest("Work", "#FF0000"))
            found <- repository.findByIds(List(category.id, -1L, -2L))
        } yield {
            assertEquals(found.length, 1)
            assertEquals(found.head.name, "Work")
        }
    }

    test("update should modify existing category and return updated version") {
        val createRequest = CategoryCreateRequest("Original", "#FF0000")

        for {
            original <- repository.create(createRequest)
            updatedCategory = original.copy(
                name = "Updated",
                color = "#00FF00"
            )
            updateResult <- repository.update(updatedCategory)
            found <- repository.findById(original.id)
        } yield {
            assert(updateResult.isDefined)
            assertEquals(updateResult.get.name, "Updated")
            assertEquals(updateResult.get.color, "#00FF00")
            assertEquals(found, updateResult)
            assert(updateResult.get.updatedAt.isAfter(original.updatedAt))
        }
    }

    test("update should return None when trying to update non-existent category") {
        val nonExistentCategory = model.Category(
            id = -1L,
            name = "Doesn't exist",
            color = "#000000",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        repository.update(nonExistentCategory).assertEquals(None)
    }

    test("delete should return true when category is deleted") {
        val createRequest = CategoryCreateRequest("To delete", "#FF0000")

        for {
            created <- repository.create(createRequest)
            deleteResult <- repository.delete(created.id)
            foundAfterDelete <- repository.findById(created.id)
        } yield {
            assertEquals(deleteResult, true)
            assertEquals(foundAfterDelete, None)
        }
    }

    test("delete should return false when category does not exist") {
        val nonExistentId = -1L
        repository.delete(nonExistentId).assertEquals(false)
    }

    test("category names should be unique") {
        val createRequest = CategoryCreateRequest("Duplicate", "#FF0000")

        for {
            first <- repository.create(createRequest)
            secondAttempt <- repository.create(createRequest).attempt
        } yield {
            assert(first.id > 0)
            assert(secondAttempt.isLeft)
        }
    }
}