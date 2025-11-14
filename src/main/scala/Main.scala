import cats.effect.{IO, IOApp}
import cats.syntax.semigroupk.*
import com.comcast.ip4s.*
import config.Config
import db.Database
import db.migrations.Migrations
import error.{ErrorHandler, ErrorResponse, InvalidId, TodoNotFound, CategoryNotFound}
import codecs.ToDoCodecs.given
import codecs.CategoryCodecs.given
import io.circe.syntax.*
import model.{CreateTodoRequest, PatchTodoRequest, UpdateTodoRequest, CategoryCreateRequest, CategoryUpdateRequest, CategoryPatchRequest}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.{HttpApp, HttpRoutes}
import repository.{TodoRepositoryPostgres, CategoryRepositoryPostgres}
import service.{TodoService, CategoryService}

import scala.util.Try

object Main extends IOApp.Simple {

    import validator.Validator.{validate as validateTodo, *}
    import validator.CategoryValidator.{validate as validateCategory, *}

    private def parseLong(id: String): IO[Long] = {
        IO.fromTry(
                Try(id.toLong))
            .adaptError {
                case _: NumberFormatException => InvalidId(id)
            }
    }

    private def createRoutes(todoService: TodoService, categoryService: CategoryService): HttpRoutes[IO] = {
        val helloWorldRoutes = HttpRoutes.of[IO] {
            case GET -> Root / "hello" =>
                Ok("Hello, World!")
        }

        val todoRoutes = HttpRoutes.of[IO] {

            case GET -> Root / "todos" =>
                for {
                    todos <- todoService.getAllTodos()
                    resp <- Ok(todos.asJson)
                } yield resp

            case GET -> Root / "todos" / id =>
                for {
                    longId <- parseLong(id)
                    todoOpt <- todoService.getTodoById(longId)
                    todo <- IO.fromOption(todoOpt)(TodoNotFound(longId))
                    resp <- Ok(todo.asJson)
                } yield resp

            case req @ POST -> Root / "todos" =>
                for {
                    createReq <- req.as[CreateTodoRequest]
                    _ <- validateTodo(createReq)
                    todo <- todoService.createTodo(createReq)
                    resp <- Created(todo.asJson)
                } yield resp

            case req @ PUT -> Root / "todos" / id =>
                for {
                    longId <- parseLong(id)
                    updateReq <- req.as[UpdateTodoRequest]
                    _ <- validateTodo(updateReq)
                    todoOpt <- todoService.updateTodo(longId, updateReq)
                    todo <- IO.fromOption(todoOpt)(TodoNotFound(longId))
                    resp <- Ok(todo.asJson)
                } yield resp

            case req @ PATCH -> Root / "todos" / id =>
                for {
                    longId <- parseLong(id)
                    patchReq <- req.as[PatchTodoRequest]
                    _ <- validateTodo(patchReq)
                    todoOpt <- todoService.patchTodo(longId, patchReq)
                    todo <- IO.fromOption(todoOpt)(TodoNotFound(longId))
                    resp <- Ok(todo.asJson)
                } yield resp

            case DELETE -> Root / "todos" / id =>
                for {
                    longId <- parseLong(id)
                    deleted <- todoService.deleteTodo(longId)
                    resp <- if (deleted) NoContent() else IO.raiseError(TodoNotFound(longId))
                } yield resp
        }

        val categoryRoutes = HttpRoutes.of[IO] {

            case GET -> Root / "categories" =>
                for {
                    categories <- categoryService.getAllCategories()
                    resp <- Ok(categories.asJson)
                } yield resp

            case GET -> Root / "categories" / id =>
                for {
                    longId <- parseLong(id)
                    categoryOpt <- categoryService.getCategoryById(longId)
                    category <- IO.fromOption(categoryOpt)(CategoryNotFound(longId))
                    resp <- Ok(category.asJson)
                } yield resp

            case req @ POST -> Root / "categories" =>
                for {
                    createReq <- req.as[CategoryCreateRequest]
                    _ <- validateCategory(createReq)
                    category <- categoryService.createCategory(createReq)
                    resp <- Created(category.asJson)
                } yield resp

            case req @ PUT -> Root / "categories" / id =>
                for {
                    longId <- parseLong(id)
                    updateReq <- req.as[CategoryUpdateRequest]
                    _ <- validateCategory(updateReq)
                    categoryOpt <- categoryService.updateCategory(longId, updateReq)
                    category <- IO.fromOption(categoryOpt)(CategoryNotFound(longId))
                    resp <- Ok(category.asJson)
                } yield resp

            case req @ PATCH -> Root / "categories" / id =>
                for {
                    longId <- parseLong(id)
                    patchReq <- req.as[CategoryPatchRequest]
                    _ <- validateCategory(patchReq)
                    categoryOpt <- categoryService.patchCategory(longId, patchReq)
                    category <- IO.fromOption(categoryOpt)(CategoryNotFound(longId))
                    resp <- Ok(category.asJson)
                } yield resp

            case DELETE -> Root / "categories" / id =>
                for {
                    longId <- parseLong(id)
                    deleted <- categoryService.deleteCategory(longId)
                    resp <- if (deleted) NoContent() else IO.raiseError(CategoryNotFound(longId))
                } yield resp
        }

        helloWorldRoutes <+> todoRoutes <+> categoryRoutes
    }

    private def notFoundApp: HttpApp[IO] = HttpRoutes.of[IO] {
        case req =>
            NotFound(ErrorResponse(s"Endpoint not found: ${req.method} ${req.uri.path}"))
    }.orNotFound

    def run: IO[Unit] = {
        for {
            config <- Config.load()
            _ = println(s"Config loaded: $config")

            _ <- Migrations.run(config.db)

            _ <- Database.transactor(config.db).use { xa =>
                val todoRepository = TodoRepositoryPostgres(xa)
                val categoryRepository = CategoryRepositoryPostgres(xa)

                val todoService = new TodoService(todoRepository,categoryRepository)
                val categoryService = new CategoryService(categoryRepository)

                val routes = createRoutes(todoService, categoryService)

                val httpApp: HttpApp[IO] = routes.orNotFound

                val handledApp = ErrorHandler(httpApp)

                EmberServerBuilder
                    .default[IO]
                    .withHost(ipv4"0.0.0.0")
                    .withPort(config.httpPort)
                    .withHttpApp(handledApp)
                    .build
                    .use(_ => IO.never)
            }
        } yield ()
    }
}