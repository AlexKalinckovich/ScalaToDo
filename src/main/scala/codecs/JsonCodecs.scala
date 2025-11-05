package codecs

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.*
import java.util.UUID
import java.time.{Instant, LocalDate}
import java.time.format.DateTimeParseException
import scala.util.Try
import model.{CreateTodoRequest, Importance, UpdateTodoRequest, TodoResponse}

object JsonCodecs {

    given uuidEncoder: Encoder[UUID] = Encoder.encodeString.contramap[UUID](_.toString)
    given uuidDecoder: Decoder[UUID] = Decoder.decodeString.map(UUID.fromString)

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
    given todoResponseEncoder: Encoder[TodoResponse] = deriveEncoder
}