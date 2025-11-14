package validator

import cats.data.ValidatedNel
import cats.effect.IO
import cats.implicits._
import error.ValidationErrors
import model.{CategoryCreateRequest, CategoryUpdateRequest, CategoryPatchRequest}

object CategoryValidator {

    private type ValidationResult[A] = ValidatedNel[String, A]

    private val MIN_NAME_LENGTH: Int = 1
    private val MAX_NAME_LENGTH: Int = 100
    private val COLOR_PATTERN: String = "^#[0-9A-Fa-f]{6}$" // Hex color format

    private def validateName(name: String): ValidationResult[String] = {
        if (name.length >= MIN_NAME_LENGTH && name.length <= MAX_NAME_LENGTH) {
            name.validNel
        } else {
            s"Category name must be between $MIN_NAME_LENGTH and $MAX_NAME_LENGTH characters".invalidNel
        }
    }

    private def validateColor(color: String): ValidationResult[String] = {
        if (color.matches(COLOR_PATTERN)) {
            color.validNel
        } else {
            "Color must be in hex format (e.g., #FF0000)".invalidNel
        }
    }

    private def lift[A](result: ValidationResult[A]): IO[A] = {
        result.toEither.fold(
            errors => IO.raiseError(ValidationErrors(errors)),
            value => IO.pure(value)
        )
    }

    def validate(req: CategoryCreateRequest): IO[CategoryCreateRequest] = {
        val result = (
            validateName(req.name),
            validateColor(req.color)
        ).mapN((_, _) => req)

        lift(result)
    }

    def validate(req: CategoryUpdateRequest): IO[CategoryUpdateRequest] = {
        val result = (
            validateName(req.name),
            validateColor(req.color)
        ).mapN((_, _) => req)

        lift(result)
    }

    def validate(req: CategoryPatchRequest): IO[CategoryPatchRequest] = {
        val nameValidation = req.name.traverse(validateName)
        val colorValidation = req.color.traverse(validateColor)

        val result = (nameValidation, colorValidation).mapN((_, _) => req)
        lift(result)
    }
}