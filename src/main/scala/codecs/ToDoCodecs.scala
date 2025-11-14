package codecs

import error.ErrorResponse
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import java.time.{Instant, LocalDate}
import java.time.format.DateTimeParseException
import scala.util.Try
import model.{CreateTodoRequest, Importance, TodoResponse, UpdateTodoRequest, PatchTodoRequest}
import codecs.CategoryCodecs.given 


object ToDoCodecs {

    given longEncoder: Encoder[Long] = Encoder.encodeLong
    given longDecoder: Decoder[Long] = Decoder.decodeLong

    given instantEncoder: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)
    given instantDecoder: Decoder[Instant] = Decoder.decodeString.map(Instant.parse)

    given localDateEncoder: Encoder[LocalDate] = Encoder.encodeString.contramap[LocalDate](_.toString)

    given localDateDecoder: Decoder[LocalDate] = Decoder.decodeString.emap { s =>
        Try(LocalDate.parse(s)).toEither.left.map {
            case _: DateTimeParseException => s"Invalid date format for '$s'. Expected format: YYYY-MM-DD"
            case e => s"Invalid date: ${e.getMessage}"
        }
    }

    given importanceEncoder: Encoder[Importance] = Encoder.encodeString.contramap[Importance](_.toString)

    given importanceDecoder: Decoder[Importance] = Decoder.decodeString.emap { s =>
        Try(Importance.valueOf(s)).toEither.left.map { _ =>
            val validValues = Importance.values.map(_.toString).mkString(", ")
            s"Invalid value '$s' for importance. Valid values are: $validValues"
        }
    }

    given createTodoRequestDecoder: Decoder[CreateTodoRequest] = deriveDecoder
    given updateTodoRequestDecoder: Decoder[UpdateTodoRequest] = deriveDecoder
    given patchTodoRequestDecoder: Decoder[PatchTodoRequest] = deriveDecoder
    given todoResponseEncoder: Encoder[TodoResponse] = deriveEncoder

    given errorResponseEncoder: Encoder[ErrorResponse] = deriveEncoder
}