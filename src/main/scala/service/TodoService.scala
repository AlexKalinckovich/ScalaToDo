package service

import cats.effect.IO
import cats.implicits._
import repository.{TodoRepository, CategoryRepository}
import model.{CreateTodoRequest, Importance, PatchTodoRequest, Todo, TodoResponse, UpdateTodoRequest}

import java.time.Instant

class TodoService(todoRepository: TodoRepository, categoryRepository: CategoryRepository) {

    def getAllTodos(): IO[List[TodoResponse]] =
        for {
            todos <- todoRepository.findAll()
            enrichedTodos <- enrichTodosWithCategories(todos)
        } yield enrichedTodos.map(toResponse)

    def getTodoById(id: Long): IO[Option[TodoResponse]] =
        todoRepository.findById(id).flatMap {
            case None => IO.pure(None)
            case Some(todo) => enrichTodoWithCategory(todo).map(t => Some(toResponse(t)))
        }

    def createTodo(request: CreateTodoRequest): IO[TodoResponse] =
        todoRepository.create(request).flatMap(enrichTodoWithCategory).map(toResponse)

    def patchTodo(id: Long, request: PatchTodoRequest): IO[Option[TodoResponse]] = {
        todoRepository.findById(id).flatMap {
            case None =>
                IO.pure(None)
            case Some(existingTodo) =>
                val patchedTodo = patch(existingTodo, request)
                todoRepository.update(patchedTodo).flatMap {
                    case None => IO.pure(None)
                    case Some(updatedTodo) => enrichTodoWithCategory(updatedTodo).map(t => Some(toResponse(t)))
                }
        }
    }

    def updateTodo(id: Long, request: UpdateTodoRequest): IO[Option[TodoResponse]] = {
        todoRepository.findById(id).flatMap {
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
                todoRepository.update(updatedTodo).flatMap {
                    case None => IO.pure(None)
                    case Some(updatedTodo) => enrichTodoWithCategory(updatedTodo).map(t => Some(toResponse(t)))
                }
        }
    }

    def deleteTodo(id: Long): IO[Boolean] =
        todoRepository.delete(id)

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

    private def enrichTodosWithCategories(todos: List[Todo]): IO[List[Todo]] = {
        val categoryIds = todos.flatMap(_.categoryId).distinct
        if (categoryIds.isEmpty) {
            IO.pure(todos)
        } else {
            categoryRepository.findByIds(categoryIds).map { categories =>
                val categoryMap = categories.map(c => c.id -> c).toMap
                todos.map { todo =>
                    todo.categoryId.flatMap(categoryMap.get) match {
                        case Some(category) => todo.copy(category = Some(category))
                        case None => todo
                    }
                }
            }
        }
    }

    private def enrichTodoWithCategory(todo: Todo): IO[Todo] = {
        todo.categoryId match {
            case Some(categoryId) =>
                categoryRepository.findById(categoryId).map {
                    case Some(category) => todo.copy(category = Some(category))
                    case None => todo
                }
            case None =>
                IO.pure(todo)
        }
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