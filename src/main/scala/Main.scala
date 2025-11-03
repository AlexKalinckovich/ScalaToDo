import cats.effect.{IO, IOApp}
import cats.syntax.semigroupk.*
import com.comcast.ip4s.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.{HttpApp, HttpRoutes}

import java.util.UUID
import scala.util.Try

object Main extends IOApp.Simple {

    import JsonCodecs.*
    import Validator.*

    private def parseUuid(id: String): IO[UUID] = {
        IO.fromTry(
                Try(UUID.fromString(id)))
            .adaptError {
                case _: IllegalArgumentException => InvalidUuid(id)
            }
    }

    private def createRoutes(todoService: TodoService): HttpRoutes[IO] = {
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
                    uuid <- parseUuid(id)
                    todoOpt <- todoService.getTodoById(uuid)
                    todo <- IO.fromOption(todoOpt)(TodoNotFound(uuid))
                    resp <- Ok(todo.asJson)
                } yield resp

            case req @ POST -> Root / "todos" =>
                for {
                    createReq <- req.as[CreateTodoRequest]
                    _ <- validate(createReq)
                    todo <- todoService.createTodo(createReq)
                    resp <- Created(todo.asJson)
                } yield resp

            case req @ PUT -> Root / "todos" / id =>
                for {
                    uuid <- parseUuid(id)
                    updateReq <- req.as[UpdateTodoRequest]
                    _ <- validate(updateReq)
                    todoOpt <- todoService.updateTodo(uuid, updateReq)
                    todo <- IO.fromOption(todoOpt)(TodoNotFound(uuid))
                    resp <- Ok(todo.asJson)
                } yield resp

            case DELETE -> Root / "todos" / id =>
                for {
                    uuid <- parseUuid(id)
                    deleted <- todoService.deleteTodo(uuid)
                    resp <- if (deleted) NoContent() else IO.raiseError(TodoNotFound(uuid))
                } yield resp
        }

        helloWorldRoutes <+> todoRoutes
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
                val todoService = new TodoService(todoRepository)

                val routes = createRoutes(todoService)

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