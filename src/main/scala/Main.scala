import cats.effect.{IO, IOApp}
import cats.syntax.semigroupk.*
import com.comcast.ip4s.*
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.* 

object Main extends IOApp.Simple {

    private val helloWorldRoutes = HttpRoutes.of[IO] {
        case GET -> Root / "hello" =>
            Ok("Hello, World!")
    }

    private val todoRoutes = HttpRoutes.of[IO] {
        case GET -> Root / "todos" =>
            Ok("Mock: Get ALL todos")
        case GET -> Root / "todos" / id =>
            Ok(s"Mock: Get todo by ID: $id")
        case POST -> Root / "todos" =>
            Ok("Mock: Create a new todo")
        case PUT -> Root / "todos" / id =>
            Ok(s"Mock: Update todo by ID: $id")
        case DELETE -> Root / "todos" / id =>
            Ok(s"Mock: Delete todo by ID: $id")
    }

    private val allRoutes = helloWorldRoutes <+> todoRoutes

    def run: IO[Unit] = {
        for {
            config <- Config.load()
            _ = println(s"Config loaded: $config")

            _ <- Migrations.run(config.db)

            _ <- EmberServerBuilder
                .default[IO]
                .withHost(ipv4"0.0.0.0")
                .withPort(config.httpPort)
                .withHttpApp(allRoutes.orNotFound)
                .build
                .use(_ => IO.never)

        } yield () 
    }
}