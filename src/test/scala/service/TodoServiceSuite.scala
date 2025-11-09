package service

import cats.effect.IO
import model.*
import munit.CatsEffectSuite
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import repository.TodoRepository

import java.time.Instant
import java.util.UUID

class TodoServiceSuite extends CatsEffectSuite with MockitoSugar {

    val testId: UUID = UUID.randomUUID()
    val now: Instant = Instant.now()

    val testTodo: Todo = Todo(
        id = testId,
        description = "Test todo",
        completed = false,
        createdAt = now,
        updatedAt = now,
        importance = Importance.Medium,
        deadline = None
    )

    val testResponse: TodoResponse = TodoResponse(
        id = testId,
        description = "Test todo",
        completed = false,
        createdAt = now,
        updatedAt = now,
        importance = Importance.Medium,
        deadline = None
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
            deadline = None
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

    test("updateTodo should return None when todo is not found") {
        val mockRepo = mock[TodoRepository]
        val todoService = new TodoService(mockRepo)

        val patchRequest = PatchTodoRequest(description = Some("New"), None, None, None)

        when(mockRepo.findById(testId)).thenReturn(IO.pure(None))

        todoService.patchTodo(testId, patchRequest).assertEquals(None)
    }

    test("updateTodo should correctly patch fields and return updated todo") {
        val mockRepo = mock[TodoRepository]
        val todoService = new TodoService(mockRepo)

        val existingTodo = Todo(
            id = testId,
            description = "Old description",
            completed = false,
            createdAt = now,
            updatedAt = now,
            importance = Importance.Low,
            deadline = None
        )

        val patchRequest = PatchTodoRequest(
            description = Some("New description"),
            completed = Some(true),
            importance = None,
            deadline = None
        )

        val patchedTodo = existingTodo.copy(
            description = "New description",
            completed = true
        )

        val expectedResponse = TodoResponse(
            id = testId,
            description = "New description",
            completed = true,
            createdAt = now,
            updatedAt = now,
            importance = Importance.Low,
            deadline = None
        )

        when(mockRepo.findById(testId)).thenReturn(IO.pure(Some(existingTodo)))
        when(mockRepo.update(patchedTodo)).thenReturn(IO.pure(Some(patchedTodo)))

        todoService.patchTodo(testId, patchRequest).assertEquals(Some(expectedResponse))
    }
}