package studio.sandlight.app.web

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import studio.sandlight.app.domain.User
import java.time.Instant

/**
 * Request/response DTOs decouple the wire format from the JPA schema.
 * Bean Validation lives on the request DTO — the single authoritative place —
 * rather than being duplicated on the entity where nothing would trigger it.
 */
data class CreateUserRequest(
    @field:NotBlank val name: String,
    @field:Email val email: String,
)

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val createdAt: Instant,
)

fun User.toResponse() = UserResponse(
    id = checkNotNull(id) { "entity must be persisted before mapping" },
    name = name,
    email = email,
    createdAt = createdAt,
)
