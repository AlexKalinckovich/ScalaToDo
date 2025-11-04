package codec

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.munit.TestContainerForAll
import db.Database
import db.migrations.Migrations
import doobie.implicits.*
import doobie.util.transactor.Transactor
import model.{CreateTodoRequest, Importance, Todo}
import munit.CatsEffectSuite
import repository.TodoRepositoryPostgres

import java.time.{Instant, LocalDate}
import java.util.UUID

class TodoRepositoryPostgresSuite extends CatsEffectSuite with TestContainerForAll {

    override val containerDef: PostgreSQLContainer.Def =
        PostgreSQLContainer.Def(dockerImageName = "postgres:16-alpine")

    private var transactor: Transactor[IO] = _
    private var repository: TodoRepositoryPostgres = _
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

                val setup: IO[(Transactor[IO], TodoRepositoryPostgres)] = for {
                    _ <- Migrations.run(dbConfig)
                    (xa, fin) <- Database.transactor(dbConfig).allocated
                    _ <- IO { finalizer = fin }
                } yield (xa, TodoRepositoryPostgres(xa))

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
            sql"DELETE FROM todos".update.run.transact(transactor).unsafeRunSync()
        }
    }

    test("findAll should return empty list when no todos exist") {
        repository.findAll().assertEquals(Nil)
    }

    test("create should insert a new todo and return it with generated id") {
        val createRequest = CreateTodoRequest(
            description = "Test todo",
            importance = Importance.Medium,
            deadline = Some(LocalDate.of(2025, 1, 10))
        )

        for {
            created <- repository.create(createRequest)
            all <- repository.findAll()
        } yield {
            assertEquals(created.description, "Test todo")
            assertEquals(created.importance, Importance.Medium)
            assertEquals(created.deadline, Some(LocalDate.of(2025, 1, 10)))
            assertEquals(created.completed, false)
            assert(created.id != null)
            assert(created.createdAt != null)
            assert(created.updatedAt != null)
            assertEquals(all.length, 1)
            assertEquals(all.head.description, "Test todo")
        }
    }

    test("findById should return Some(todo) when todo exists") {
        val createRequest = CreateTodoRequest("Find me", Importance.High, None)

        for {
            created <- repository.create(createRequest)
            found <- repository.findById(created.id)
        } yield {
            assert(found.isDefined)
            assertEquals(found.get.id, created.id)
            assertEquals(found.get.description, "Find me")
            assertEquals(found.get.importance, Importance.High)
        }
    }

    test("findById should return None when todo does not exist") {
        val nonExistentId = UUID.randomUUID()
        repository.findById(nonExistentId).assertEquals(None)
    }

    test("update should modify existing todo and return updated version") {
        val createRequest = CreateTodoRequest("Original", Importance.Medium, None)

        for {
            original <- repository.create(createRequest)
            updatedTodo = original.copy(
                description = "Updated",
                completed = true,
                importance = Importance.High
            )
            updateResult <- repository.update(updatedTodo)
            found <- repository.findById(original.id)
        } yield {
            assert(updateResult.isDefined)
            assertEquals(updateResult.get.description, "Updated")
            assertEquals(updateResult.get.completed, true)
            assertEquals(updateResult.get.importance, Importance.High)
            assertEquals(found, updateResult)
            assert(updateResult.get.updatedAt.isAfter(original.updatedAt))
        }
    }

    test("update should return None when trying to update non-existent todo") {
        val nonExistentTodo = Todo(
            id = UUID.randomUUID(),
            description = "Doesn't exist",
            completed = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            importance = Importance.Medium,
            deadline = None
        )

        repository.update(nonExistentTodo).assertEquals(None)
    }

    test("delete should return true when todo is deleted") {
        val createRequest = CreateTodoRequest("To delete", Importance.Low, None)

        for {
            created <- repository.create(createRequest)
            deleteResult <- repository.delete(created.id)
            foundAfterDelete <- repository.findById(created.id)
        } yield {
            assertEquals(deleteResult, true)
            assertEquals(foundAfterDelete, None)
        }
    }

    test("delete should return false when todo does not exist") {
        val nonExistentId = UUID.randomUUID()
        repository.delete(nonExistentId).assertEquals(false)
    }
}
