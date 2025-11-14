package service

import cats.effect.IO
import repository.TodoRepository
import model.{CreateTodoRequest, Importance, PatchTodoRequest, Todo, TodoResponse, UpdateTodoRequest}

import java.time.Instant

class TodoService(repository: TodoRepository) {

    def getAllTodos(): IO[List[TodoResponse]] =
        repository.findAll().map(_.map(toResponse))

    def getTodoById(id: Long): IO[Option[TodoResponse]] =
        repository.findById(id).map(_.map(toResponse))

    def createTodo(request: CreateTodoRequest): IO[TodoResponse] =
        repository.create(request).map(toResponse)

    def patchTodo(id: Long, request: PatchTodoRequest): IO[Option[TodoResponse]] = {
        repository.findById(id).flatMap {
            case None =>
                IO.pure(None)
            case Some(existingTodo) =>
                val patchedTodo = patch(existingTodo, request)
                repository.update(patchedTodo).map(_.map(toResponse))
        }
    }

    def updateTodo(id: Long, request: UpdateTodoRequest): IO[Option[TodoResponse]] = {
        repository.findById(id).flatMap {
            case None =>
                IO.pure(None)
            case Some(existingTodo) =>
                val updatedTodo = existingTodo.copy(
                    description = request.description,
                    completed = request.completed,
                    importance = request.importance,
                    deadline = request.deadline,
                    categoryId = request.categoryId,
                    updatedAt = Instant.now()
                )
                repository.update(updatedTodo).map(_.map(toResponse))
        }
    }

    def deleteTodo(id: Long): IO[Boolean] =
        repository.delete(id)

    private def patch(existing: Todo, request: PatchTodoRequest): Todo = {
        existing.copy(
            description = request.description.getOrElse(existing.description),
            completed = request.completed.getOrElse(existing.completed),
            importance = request.importance.getOrElse(existing.importance),
            deadline = request.deadline.orElse(existing.deadline),
            categoryId = request.categoryId.orElse(existing.categoryId),
            updatedAt = Instant.now()
        )
    }

    private def toResponse(todo: Todo): TodoResponse =
        TodoResponse(
            id = todo.id,
            description = todo.description,
            completed = todo.completed,
            createdAt = todo.createdAt,
            updatedAt = todo.updatedAt,
            importance = todo.importance,
            deadline = todo.deadline,
            category = todo.category
        )
}