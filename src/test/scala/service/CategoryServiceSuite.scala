package service

import cats.effect.IO
import model.*
import munit.CatsEffectSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import repository.CategoryRepository

import java.time.Instant

class CategoryServiceSuite extends CatsEffectSuite with MockitoSugar {

    val testId: Long = 1L
    val now: Instant = Instant.now()

    val testCategory: Category = Category(
        id = testId,
        name = "Work",
        color = "#FF0000",
        createdAt = now,
        updatedAt = now
    )

    val testResponse: CategoryResponse = CategoryResponse(
        id = testId,
        name = "Work",
        color = "#FF0000",
        createdAt = now,
        updatedAt = now
    )

    test("getAllCategories should return all category responses") {
        val mockRepo = mock[CategoryRepository]
        val categoryService = new CategoryService(mockRepo)

        val categories = List(testCategory)
        val expectedResponses = List(testResponse)

        when(mockRepo.findAll()).thenReturn(IO.pure(categories))

        categoryService.getAllCategories().assertEquals(expectedResponses)
    }

    test("getCategoryById should return a category response when found") {
        val mockRepo = mock[CategoryRepository]
        val categoryService = new CategoryService(mockRepo)

        when(mockRepo.findById(testId)).thenReturn(IO.pure(Some(testCategory)))

        categoryService.getCategoryById(testId).assertEquals(Some(testResponse))
    }

    test("getCategoryById should return None when not found") {
        val mockRepo = mock[CategoryRepository]
        val categoryService = new CategoryService(mockRepo)

        when(mockRepo.findById(testId)).thenReturn(IO.pure(None))

        categoryService.getCategoryById(testId).assertEquals(None)
    }

    test("createCategory should create and return a category response") {
        val mockRepo = mock[CategoryRepository]
        val categoryService = new CategoryService(mockRepo)

        val createRequest = CategoryCreateRequest(
            name = "Work",
            color = "#FF0000"
        )

        when(mockRepo.create(createRequest)).thenReturn(IO.pure(testCategory))

        categoryService.createCategory(createRequest).assertEquals(testResponse)
    }

    test("deleteCategory should return true when deleted") {
        val mockRepo = mock[CategoryRepository]
        val categoryService = new CategoryService(mockRepo)

        when(mockRepo.delete(testId)).thenReturn(IO.pure(true))

        categoryService.deleteCategory(testId).assertEquals(true)
    }

    test("deleteCategory should return false when not found") {
        val mockRepo = mock[CategoryRepository]
        val categoryService = new CategoryService(mockRepo)

        when(mockRepo.delete(testId)).thenReturn(IO.pure(false))

        categoryService.deleteCategory(testId).assertEquals(false)
    }

    test("patchCategory should return None when category is not found") {
        val mockRepo = mock[CategoryRepository]
        val categoryService = new CategoryService(mockRepo)

        val patchRequest = CategoryPatchRequest(
            name = Some("New Name"),
            color = None
        )

        when(mockRepo.findById(testId)).thenReturn(IO.pure(None))

        categoryService.patchCategory(testId, patchRequest).assertEquals(None)
    }

    test("patchCategory should correctly patch fields and return updated category") {
        val mockRepo = mock[CategoryRepository]
        val categoryService = new CategoryService(mockRepo)

        val existingCategory = Category(
            id = testId,
            name = "Old Name",
            color = "#FF0000",
            createdAt = now,
            updatedAt = now
        )

        val patchRequest = CategoryPatchRequest(
            name = Some("New Name"),
            color = Some("#00FF00")
        )

        val patchedCategory = existingCategory.copy(
            name = "New Name",
            color = "#00FF00",
            updatedAt = now
        )

        val expectedResponse = CategoryResponse(
            id = testId,
            name = "New Name",
            color = "#00FF00",
            createdAt = now,
            updatedAt = now
        )

        when(mockRepo.findById(testId)).thenReturn(IO.pure(Some(existingCategory)))
        when(mockRepo.update(any)).thenReturn(IO.pure(Some(patchedCategory)))

        categoryService.patchCategory(testId, patchRequest).assertEquals(Some(expectedResponse))
    }

    test("patchCategory should handle partial updates correctly") {
        val mockRepo = mock[CategoryRepository]
        val categoryService = new CategoryService(mockRepo)

        val existingCategory: Category = Category(
            id = testId,
            name = "Old Name",
            color = "#FF0000",
            createdAt = now,
            updatedAt = now
        )

        val patchRequest: CategoryPatchRequest = CategoryPatchRequest(
            name = Some("New Name"),
            color = None
        )

        val patchedCategory: Category = existingCategory.copy(
            name = "New Name",
            updatedAt = now
        )

        val expectedResponse: CategoryResponse = CategoryResponse(
            id = testId,
            name = "New Name",
            color = "#FF0000",
            createdAt = now,
            updatedAt = now
        )

        when(mockRepo.findById(testId)).thenReturn(IO.pure(Some(existingCategory)))
        when(mockRepo.update(any())).thenReturn(IO.pure(Some(patchedCategory)))

        categoryService.patchCategory(testId, patchRequest).assertEquals(Some(expectedResponse))
    }

    test("updateCategory should return None when category is not found") {
        val mockRepo = mock[CategoryRepository]
        val categoryService = new CategoryService(mockRepo)

        val updateRequest = CategoryUpdateRequest(
            name = "New Name",
            color = "#00FF00"
        )

        when(mockRepo.findById(testId)).thenReturn(IO.pure(None))

        categoryService.updateCategory(testId, updateRequest).assertEquals(None)
    }

    test("updateCategory should update category and return response") {
        val mockRepo = mock[CategoryRepository]
        val categoryService = new CategoryService(mockRepo)

        val existingCategory: Category = Category(
            id = testId,
            name = "Old Name",
            color = "#FF0000",
            createdAt = now,
            updatedAt = now
        )

        val updateRequest: CategoryUpdateRequest = CategoryUpdateRequest(
            name = "New Name",
            color = "#00FF00"
        )

        val updatedCategory: Category = existingCategory.copy(
            name = "New Name",
            color = "#00FF00",
            updatedAt = now
        )

        val expectedResponse: CategoryResponse = CategoryResponse(
            id = testId,
            name = "New Name",
            color = "#00FF00",
            createdAt = now,
            updatedAt = now
        )

        when(mockRepo.findById(testId)).thenReturn(IO.pure(Some(existingCategory)))
        when(mockRepo.update(any())).thenReturn(IO.pure(Some(updatedCategory)))

        categoryService.updateCategory(testId, updateRequest).assertEquals(Some(expectedResponse))
    }
}