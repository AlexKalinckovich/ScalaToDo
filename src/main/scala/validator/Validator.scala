package validator

import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.effect.IO
import cats.implicits.*
import cats.syntax.validated.catsSyntaxValidatedId
import error.ValidationErrors
import model.{CreateTodoRequest, UpdateTodoRequest}

object Validator {

    private type ValidationResult[A] = ValidatedNel[String, A]

    private val MIN_DESCRIPTION_LENGTH : Int = 1
    private val MAX_DESCRIPTION_LENGTH : Int = 512

    private def validateDescription(desc: String): ValidationResult[String] = {
        if (desc.length >= MIN_DESCRIPTION_LENGTH && desc.length <= MAX_DESCRIPTION_LENGTH) {
            desc.validNel
        } else {
            s"Description must be between $MIN_DESCRIPTION_LENGTH and $MAX_DESCRIPTION_LENGTH characters".invalidNel
        }
    }

    private def lift[A](result: ValidationResult[A]): IO[A] = {
        result.toEither.fold(
            errors => IO.raiseError(ValidationErrors(errors)), 
            value => IO.pure(value)                           
        )
    }

    def validate(req: CreateTodoRequest): IO[CreateTodoRequest] = {
        val result = validateDescription(req.description).map(_ => req)
        lift(result)
    }

    def validate(req: UpdateTodoRequest): IO[UpdateTodoRequest] = {
        val result = req.description.traverse_(validateDescription)
        lift(result.map(_ => req))
    }
}