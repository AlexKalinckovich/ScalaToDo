package service

import cats.effect.IO
import repository.CategoryRepository
import model.{Category, CategoryCreateRequest, CategoryPatchRequest, CategoryResponse, CategoryUpdateRequest}

import java.time.Instant

class CategoryService(repository: CategoryRepository) {

    def getAllCategories(): IO[List[CategoryResponse]] =
        repository.findAll().map(_.map(toResponse))

    def getCategoryById(id: Long): IO[Option[CategoryResponse]] =
        repository.findById(id).map(_.map(toResponse))

    def createCategory(request: CategoryCreateRequest): IO[CategoryResponse] =
        repository.create(request).map(toResponse)

    def updateCategory(id: Long, request: CategoryUpdateRequest): IO[Option[CategoryResponse]] = {
        repository.findById(id).flatMap {
            case None =>
                IO.pure(None)
            case Some(existingCategory) =>
                val updatedCategory = existingCategory.copy(
                    name = request.name,
                    color = request.color,
                    updatedAt = Instant.now()
                )
                repository.update(updatedCategory).map(_.map(toResponse))
        }
    }

    def patchCategory(id: Long, request: CategoryPatchRequest): IO[Option[CategoryResponse]] = {
        repository.findById(id).flatMap {
            case None =>
                IO.pure(None)
            case Some(existingCategory) =>
                val patchedCategory = patch(existingCategory, request)
                repository.update(patchedCategory).map(_.map(toResponse))
        }
    }

    def deleteCategory(id: Long): IO[Boolean] =
        repository.delete(id)

    private def patch(existing: Category, request: CategoryPatchRequest): Category = {
        existing.copy(
            id = existing.id, 
            name = request.name.getOrElse(existing.name),
            color = request.color.getOrElse(existing.color),
            createdAt = existing.createdAt, 
            updatedAt = Instant.now()
        )
    }

    private def toResponse(category: Category): CategoryResponse =
        CategoryResponse(
            id = category.id,
            name = category.name,
            color = category.color,
            createdAt = category.createdAt,
            updatedAt = category.updatedAt
        )
}