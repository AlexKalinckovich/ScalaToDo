package db

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.munit.TestContainerForAll
import db.Database
import db.migrations.Migrations
import doobie.implicits.*
import doobie.util.transactor.Transactor
import model.{CategoryCreateRequest, CreateTodoRequest, Importance, Todo}
import munit.CatsEffectSuite
import repository.{CategoryRepositoryPostgres, TodoRepositoryPostgres}

import java.time.{Instant, LocalDate}
import scala.compiletime.uninitialized

class TodoRepositoryPostgresSuite extends CatsEffectSuite with TestContainerForAll {

    override val containerDef: PostgreSQLContainer.Def =
        PostgreSQLContainer.Def(dockerImageName = "postgres:16-alpine")

    private var transactor: Transactor[IO] = uninitialized
    private var todoRepository: TodoRepositoryPostgres = uninitialized
    private var categoryRepository: CategoryRepositoryPostgres = uninitialized
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

                val setup: IO[(Transactor[IO], TodoRepositoryPostgres, CategoryRepositoryPostgres)] = for {
                    _ <- Migrations.run(dbConfig)
                    (xa, fin) <- Database.transactor(dbConfig).allocated
                    _ <- IO { finalizer = fin }
                } yield (xa, TodoRepositoryPostgres(xa), CategoryRepositoryPostgres(xa))

                val (xa, todoRepo, categoryRepo) = setup.unsafeRunSync()
                transactor = xa
                todoRepository = todoRepo
                categoryRepository = categoryRepo
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
            sql"DELETE FROM categories".update.run.transact(transactor).unsafeRunSync()
        }
    }

    test("findAll should return empty list when no todos exist") {
        todoRepository.findAll().assertEquals(Nil)
    }

    test("create should insert a new todo and return it with generated id") {
        val createRequest = CreateTodoRequest(
            description = "Test todo",
            importance = Importance.Medium,
            deadline = Some(LocalDate.of(2025, 1, 10)),
            categoryId = None
        )

        for {
            created <- todoRepository.create(createRequest)
            all <- todoRepository.findAll()
        } yield {
            assertEquals(created.description, "Test todo")
            assertEquals(created.importance, Importance.Medium)
            assertEquals(created.deadline, Some(LocalDate.of(2025, 1, 10)))
            assertEquals(created.completed, false)
            assert(created.id > 0)
            assert(created.createdAt != null)
            assert(created.updatedAt != null)
            assertEquals(all.length, 1)
            assertEquals(all.head.description, "Test todo")
        }
    }

    test("create should insert a new todo with category") {
        for {
            category <- categoryRepository.create(CategoryCreateRequest("Work", "#FF0000"))
            todo <- todoRepository.create(CreateTodoRequest(
                description = "Work todo",
                importance = Importance.High,
                deadline = None,
                categoryId = Some(category.id)
            ))
            foundTodo <- todoRepository.findById(todo.id)
        } yield {
            assertEquals(foundTodo.get.description, "Work todo")
            assertEquals(foundTodo.get.categoryId, Some(category.id))
        }
    }

    test("findById should return Some(todo) when todo exists") {
        val createRequest = CreateTodoRequest("Find me", Importance.High, None, None)

        for {
            created <- todoRepository.create(createRequest)
            found <- todoRepository.findById(created.id)
        } yield {
            assert(found.isDefined)
            assertEquals(found.get.id, created.id)
            assertEquals(found.get.description, "Find me")
            assertEquals(found.get.importance, Importance.High)
        }
    }

    test("findById should return None when todo does not exist") {
        val nonExistentId = -1L
        todoRepository.findById(nonExistentId).assertEquals(None)
    }

    test("update should modify existing todo and return updated version") {
        val createRequest = CreateTodoRequest("Original", Importance.Medium, None, None)

        for {
            original <- todoRepository.create(createRequest)
            updatedTodo = original.copy(
                description = "Updated",
                completed = true,
                importance = Importance.High
            )
            updateResult <- todoRepository.update(updatedTodo)
            found <- todoRepository.findById(original.id)
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
            id = -1L,
            description = "Doesn't exist",
            completed = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            importance = Importance.Medium,
            deadline = None,
            categoryId = None,
            category = None
        )

        todoRepository.update(nonExistentTodo).assertEquals(None)
    }

    test("delete should return true when todo is deleted") {
        val createRequest = CreateTodoRequest("To delete", Importance.Low, None, None)

        for {
            created <- todoRepository.create(createRequest)
            deleteResult <- todoRepository.delete(created.id)
            foundAfterDelete <- todoRepository.findById(created.id)
        } yield {
            assertEquals(deleteResult, true)
            assertEquals(foundAfterDelete, None)
        }
    }

    test("delete should return false when todo does not exist") {
        val nonExistentId = -1L
        todoRepository.delete(nonExistentId).assertEquals(false)
    }
}

