package studio.sandlight.app.web

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import studio.sandlight.app.service.DuplicateEmailException

/**
 * Error responses use [ProblemDetail] (RFC 9457 "Problem Details"), the
 * standard Spring 6+ replacement for hand-rolled error DTOs. Returning
 * ProblemDetail from an @ExceptionHandler makes Spring render it as
 * `application/problem+json` with the status set once, not duplicated.
 *
 * Alternatives worth knowing: extend ResponseEntityExceptionHandler to get
 * ProblemDetail for all built-in MVC exceptions, or set
 * `spring.mvc.problemdetails.enabled=true` for framework-level defaults.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException::class)
    fun conflict(e: DuplicateEmailException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message)

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun badRequest(e: RuntimeException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.message)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(e: MethodArgumentNotValidException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed").apply {
            // Field-level detail instead of the exception's unreadable message blob.
            setProperty("errors", e.bindingResult.fieldErrors.map {
                mapOf("field" to it.field, "message" to it.defaultMessage)
            })
        }
}
