import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

import java.util.UUID
import java.time.{Instant, LocalDate}
import scala.util.Try 

object JsonCodecs {

    implicit val uuidEncoder: Encoder[UUID] = Encoder.encodeString.contramap[UUID](_.toString)
    implicit val uuidDecoder: Decoder[UUID] = Decoder.decodeString.map(UUID.fromString)

    implicit val instantEncoder: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)
    implicit val instantDecoder: Decoder[Instant] = Decoder.decodeString.map(Instant.parse)

    implicit val localDateEncoder: Encoder[LocalDate] = Encoder.encodeString.contramap[LocalDate](_.toString)
    implicit val localDateDecoder: Decoder[LocalDate] = Decoder.decodeString.map(LocalDate.parse)

    implicit val importanceEncoder: Encoder[Importance] = Encoder.encodeString.contramap[Importance](_.toString)
    implicit val importanceDecoder: Decoder[Importance] = Decoder.decodeString.emapTry(s => Try(Importance.valueOf(s)))

    implicit val createTodoRequestDecoder: Decoder[CreateTodoRequest] = deriveDecoder
    implicit val updateTodoRequestDecoder: Decoder[UpdateTodoRequest] = deriveDecoder
    implicit val todoResponseEncoder: Encoder[TodoResponse] = deriveEncoder
}