package error

import cats.data.Kleisli
import cats.effect.IO
import cats.syntax.applicativeError.catsSyntaxApplicativeError
import io.circe.{DecodingFailure, ParsingFailure}
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.{HttpApp, InvalidMessageBodyFailure, MalformedMessageBodyFailure}

case class ErrorResponse(message: String, details: Option[List[String]] = None)

object ErrorHandler {

    private val missingFieldRegex = "Missing required field: '(.*)'".r

    private def parseDecodingFailure(e: DecodingFailure): ErrorResponse = {
        val rawMessage = e.getMessage

        rawMessage match {
            case missingFieldRegex(field) =>
                ErrorResponse(s"The field '$field' is required.", None)

            case _ =>
                ErrorResponse(rawMessage, None)
        }
    }

    def apply(app: HttpApp[IO]): HttpApp[IO] = {
        app.handleErrorWith {
            case ValidationErrors(msgs) =>
                Kleisli.liftF(BadRequest(ErrorResponse("Validation failed", Some(msgs.toList))))

            case TodoNotFound(id) =>
                Kleisli.liftF(NotFound(ErrorResponse(s"Todo with id $id not found")))
                
            case e: InvalidMessageBodyFailure =>
                e.cause match {
                    case Some(df: DecodingFailure) =>
                        Kleisli.liftF(BadRequest(parseDecodingFailure(df)))
                    case Some(pf: ParsingFailure) =>
                        Kleisli.liftF(BadRequest(ErrorResponse("Invalid JSON format.", Some(List(pf.getMessage)))))
                    case _ =>
                        Kleisli.liftF(BadRequest(ErrorResponse("Invalid JSON body.", Some(List(e.getMessage)))))
                }

            case e: MalformedMessageBodyFailure =>
                Kleisli.liftF(BadRequest(ErrorResponse("Malformed JSON body.", Some(List(e.getMessage)))))

            case e: Throwable =>
                Kleisli.liftF(InternalServerError(ErrorResponse(s"Something went wrong: ${e.getMessage}")))
        }
    }
}