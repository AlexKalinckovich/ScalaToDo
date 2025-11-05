package codecs

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.*
import java.util.UUID
import java.time.{Instant, LocalDate}
import java.time.format.DateTimeParseException
import scala.util.Try
import model.{CreateTodoRequest, Importance, UpdateTodoRequest, TodoResponse}

object JsonCodecs {

    implicit val uuidEncoder: Encoder[UUID] = Encoder.encodeString.contramap[UUID](_.toString)
    implicit val uuidDecoder: Decoder[UUID] = Decoder.decodeString.map(UUID.fromString)

    implicit val instantEncoder: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)
    implicit val instantDecoder: Decoder[Instant] = Decoder.decodeString.map(Instant.parse)

    implicit val localDateEncoder: Encoder[LocalDate] = Encoder.encodeString.contramap[LocalDate](_.toString)

    implicit val localDateDecoder: Decoder[LocalDate] = Decoder.decodeString.emap { s =>
        Try(LocalDate.parse(s)).toEither.left.map {
            case _: DateTimeParseException => s"Invalid date format for '$s'. Expected format: YYYY-MM-DD"
            case e => s"Invalid date: ${e.getMessage}"
        }
    }

    implicit val importanceEncoder: Encoder[Importance] = Encoder.encodeString.contramap[Importance](_.toString)

    implicit val importanceDecoder: Decoder[Importance] = Decoder.decodeString.emap { s =>
        Try(Importance.valueOf(s)).toEither.left.map { _ =>
            val validValues = Importance.values.map(_.toString).mkString(", ")
            s"Invalid value '$s' for importance. Valid values are: $validValues"
        }
    }

    implicit val createTodoRequestDecoder: Decoder[CreateTodoRequest] = deriveDecoder
    implicit val updateTodoRequestDecoder: Decoder[UpdateTodoRequest] = deriveDecoder
    implicit val todoResponseEncoder: Encoder[TodoResponse] = deriveEncoder
}