package studio.sandlight.app.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {
    data class ErrorResponse(val status: Int, val error: String, val message: String?)

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun badRequest(e: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(400, "Bad Request", e.message))

    @ExceptionHandler(MethodArgumentNotValidException::class, BindException::class)
    fun validation(e: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ErrorResponse(422, "Validation Failed", e.message))
}

