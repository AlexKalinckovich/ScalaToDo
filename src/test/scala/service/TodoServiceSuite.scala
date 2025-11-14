package service

import cats.effect.IO
import model.*
import munit.CatsEffectSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import repository.TodoRepository

import java.time.Instant

class TodoServiceSuite extends CatsEffectSuite with MockitoSugar {

    val testId: Long = 1L
    val now: Instant = Instant.now()
    val testCategory: Category = Category(1L, "Work", "#FF0000", now, now)

    val testTodo: Todo = Todo(
        id = testId,
        description = "Test todo",
        completed = false,
        createdAt = now,
        updatedAt = now,
        importance = Importance.Medium,
        deadline = None,
        categoryId = Some(1L),
        category = Some(testCategory)
    )

    val testResponse: TodoResponse = TodoResponse(
        id = testId,
        description = "Test todo",
        completed = false,
        createdAt = now,
        updatedAt = now,
        importance = Importance.Medium,
        deadline = None,
        category = Some(testCategory)
    )

    test("getAllTodos should return all todo responses") {
        val mockRepo = mock[TodoRepository]
        val todoService = new TodoService(mockRepo)

        val todos = List(testTodo)
        val expectedResponses = List(testResponse)

        when(mockRepo.findAll()).thenReturn(IO.pure(todos))

        todoService.getAllTodos().assertEquals(expectedResponses)
    }

    test("getTodoById should return a todo response when found") {
        val mockRepo = mock[TodoRepository]
        val todoService = new TodoService(mockRepo)

        when(mockRepo.findById(testId)).thenReturn(IO.pure(Some(testTodo)))

        todoService.getTodoById(testId).assertEquals(Some(testResponse))
    }

    test("getTodoById should return None when not found") {
        val mockRepo = mock[TodoRepository]
        val todoService = new TodoService(mockRepo)

        when(mockRepo.findById(testId)).thenReturn(IO.pure(None))

        todoService.getTodoById(testId).assertEquals(None)
    }

    test("createTodo should create and return a todo response") {
        val mockRepo = mock[TodoRepository]
        val todoService = new TodoService(mockRepo)

        val createRequest = CreateTodoRequest(
            description = "Test todo",
            importance = Importance.Medium,
            deadline = None,
            categoryId = Some(1L)
        )

        when(mockRepo.create(createRequest)).thenReturn(IO.pure(testTodo))

        todoService.createTodo(createRequest).assertEquals(testResponse)
    }

    test("deleteTodo should return true when deleted") {
        val mockRepo = mock[TodoRepository]
        val todoService = new TodoService(mockRepo)

        when(mockRepo.delete(testId)).thenReturn(IO.pure(true))

        todoService.deleteTodo(testId).assertEquals(true)
    }

    test("deleteTodo should return false when not found") {
        val mockRepo = mock[TodoRepository]
        val todoService = new TodoService(mockRepo)

        when(mockRepo.delete(testId)).thenReturn(IO.pure(false))

        todoService.deleteTodo(testId).assertEquals(false)
    }

    test("patchTodo should return None when todo is not found") {
        val mockRepo = mock[TodoRepository]
        val todoService = new TodoService(mockRepo)

        val patchRequest = PatchTodoRequest(
            description = Some("New"),
            completed = None,
            importance = None,
            deadline = None,
            categoryId = None
        )

        when(mockRepo.findById(testId)).thenReturn(IO.pure(None))

        todoService.patchTodo(testId, patchRequest).assertEquals(None)
    }

    test("patchTodo should correctly patch fields and return updated todo") {
        val mockRepo = mock[TodoRepository]
        val todoService = new TodoService(mockRepo)

        val existingTodo = Todo(
            id = testId,
            description = "Old description",
            completed = false,
            createdAt = now,
            updatedAt = now,
            importance = Importance.Low,
            deadline = None,
            categoryId = Some(1L),
            category = Some(testCategory)
        )

        val patchRequest = PatchTodoRequest(
            description = Some("New description"),
            completed = Some(true),
            importance = None,
            deadline = None,
            categoryId = Some(2L)
        )

        val patchedTodo = existingTodo.copy(
            description = "New description",
            completed = true,
            categoryId = Some(2L),
            updatedAt = now
        )

        val expectedResponse = TodoResponse(
            id = testId,
            description = "New description",
            completed = true,
            createdAt = now,
            updatedAt = now,
            importance = Importance.Low,
            deadline = None,
            category = Some(testCategory)
        )

        when(mockRepo.findById(testId)).thenReturn(IO.pure(Some(existingTodo)))
        when(mockRepo.update(any())).thenReturn(IO.pure(Some(patchedTodo)))

        todoService.patchTodo(testId, patchRequest).assertEquals(Some(expectedResponse))
    }

    test("updateTodo should return None when todo is not found") {
        val mockRepo = mock[TodoRepository]
        val todoService = new TodoService(mockRepo)

        val updateRequest = UpdateTodoRequest(
            description = "New description",
            completed = true,
            importance = Importance.High,
            deadline = None,
            categoryId = Some(1L)
        )

        when(mockRepo.findById(testId)).thenReturn(IO.pure(None))

        todoService.updateTodo(testId, updateRequest).assertEquals(None)
    }

    test("updateTodo should update todo and return response") {
        val mockRepo = mock[TodoRepository]
        val todoService = new TodoService(mockRepo)

        val existingTodo = Todo(
            id = testId,
            description = "Old description",
            completed = false,
            createdAt = now,
            updatedAt = now,
            importance = Importance.Low,
            deadline = None,
            categoryId = Some(1L),
            category = Some(testCategory)
        )

        val updateRequest = UpdateTodoRequest(
            description = "New description",
            completed = true,
            importance = Importance.High,
            deadline = Some(java.time.LocalDate.of(2025, 1, 1)),
            categoryId = Some(2L)
        )

        val updatedTodo = existingTodo.copy(
            description = "New description",
            completed = true,
            importance = Importance.High,
            deadline = Some(java.time.LocalDate.of(2025, 1, 1)),
            categoryId = Some(2L),
            updatedAt = now
        )

        val expectedResponse = TodoResponse(
            id = testId,
            description = "New description",
            completed = true,
            createdAt = now,
            updatedAt = now,
            importance = Importance.High,
            deadline = Some(java.time.LocalDate.of(2025, 1, 1)),
            category = Some(testCategory)
        )

        when(mockRepo.findById(testId)).thenReturn(IO.pure(Some(existingTodo)))
        when(mockRepo.update(any())).thenReturn(IO.pure(Some(updatedTodo)))

        todoService.updateTodo(testId, updateRequest).assertEquals(Some(expectedResponse))
    }
}