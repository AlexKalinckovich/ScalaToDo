package codecs

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import model.{Category, CategoryResponse, CategoryCreateRequest, CategoryUpdateRequest, CategoryPatchRequest}

object CategoryCodecs {

    given categoryEncoder: Encoder[Category] = deriveEncoder
    given categoryDecoder: Decoder[Category] = deriveDecoder

    given categoryResponseEncoder: Encoder[CategoryResponse] = deriveEncoder
    given categoryResponseDecoder: Decoder[CategoryResponse] = deriveDecoder

    given categoryCreateRequestDecoder: Decoder[CategoryCreateRequest] = deriveDecoder
    given categoryUpdateRequestDecoder: Decoder[CategoryUpdateRequest] = deriveDecoder
    given categoryPatchRequestDecoder: Decoder[CategoryPatchRequest] = deriveDecoder
}