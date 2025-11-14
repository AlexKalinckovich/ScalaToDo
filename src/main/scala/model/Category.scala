package model

import java.time.Instant
import java.util.UUID

case class Category(
                       id: Long,
                       name: String,
                       color: String,
                       createdAt: Instant,
                       updatedAt: Instant
                   )

case class CategoryResponse(
                               id: Long,
                               name: String,
                               color: String,
                               createdAt: Instant,
                               updatedAt: Instant
                           )

case class CategoryCreateRequest(
                                    name: String,
                                    color: String
                                )

case class CategoryUpdateRequest(
                                    name: String,
                                    color: String
                                )

case class CategoryPatchRequest(
                                   name: Option[String],
                                   color: Option[String]
                               )