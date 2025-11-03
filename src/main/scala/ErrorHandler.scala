import cats.data.Kleisli
import cats.effect.IO
import cats.syntax.applicativeError.catsSyntaxApplicativeError
import io.circe.generic.auto.*
import org.http4s.{HttpApp, InvalidMessageBodyFailure, MalformedMessageBodyFailure}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*

case class ErrorResponse(message: String, details: Option[List[String]] = None)

object ErrorHandler {

    def apply(app: HttpApp[IO]): HttpApp[IO] = {
        app.handleErrorWith {
            case ValidationErrors(msgs) =>
                Kleisli.liftF(BadRequest(ErrorResponse("Validation failed", Some(msgs.toList))))

            case TodoNotFound(id) =>
                Kleisli.liftF(NotFound(ErrorResponse(s"Todo with id $id not found")))

            case InvalidUuid(raw) =>
                Kleisli.liftF(BadRequest(ErrorResponse(s"Invalid UUID: $raw")))

            case e: InvalidMessageBodyFailure =>
                Kleisli.liftF(BadRequest(ErrorResponse("Invalid JSON format or structure", Some(List(e.getMessage)))))

            case e: MalformedMessageBodyFailure =>
                Kleisli.liftF(BadRequest(ErrorResponse("Malformed JSON body", Some(List(e.getMessage)))))

            case e: Throwable =>
                Kleisli.liftF(InternalServerError(ErrorResponse(s"Something went wrong: ${e.getMessage}")))
        }
    }
}