import cats.effect.IO
import java.util.UUID

class TodoService(repository: TodoRepository) {

    def getAllTodos(): IO[List[TodoResponse]] =
        repository.findAll().map(_.map(toResponse))

    def getTodoById(id: UUID): IO[Option[TodoResponse]] =
        repository.findById(id).map(_.map(toResponse))

    def createTodo(request: CreateTodoRequest): IO[TodoResponse] =
        repository.create(request).map(toResponse)

    def updateTodo(id: UUID, request: UpdateTodoRequest): IO[Option[TodoResponse]] =
        repository.update(id, request).map(_.map(toResponse))

    def deleteTodo(id: UUID): IO[Boolean] =
        repository.delete(id)

    private def toResponse(todo: Todo): TodoResponse =
        TodoResponse(
            id = todo.id,
            description = todo.description,
            completed = todo.completed,
            createdAt = todo.createdAt,
            updatedAt = todo.updatedAt,
            importance = todo.importance,
            deadline = todo.deadline
        )
}