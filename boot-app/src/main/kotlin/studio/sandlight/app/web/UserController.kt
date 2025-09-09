package studio.sandlight.app.web

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import studio.sandlight.app.domain.User
import studio.sandlight.app.repo.UserRepository

@RestController
@RequestMapping("/api/users")
class UserController(private val repo: UserRepository) {

    @GetMapping
    fun list(): List<User> = repo.findAll()

    data class CreateUserRequest(
        @field:NotBlank val name: String,
        @field:Email val email: String,
    )

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun create(@Valid @RequestBody body: CreateUserRequest): User {
        require(!repo.existsByEmail(body.email)) { "email already exists" }
        return repo.save(User(name = body.name, email = body.email))
    }
}

