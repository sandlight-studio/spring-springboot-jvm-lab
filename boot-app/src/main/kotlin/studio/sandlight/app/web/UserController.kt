package studio.sandlight.app.web

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import studio.sandlight.app.service.UserService

@RestController
@RequestMapping("/api/users")
class UserController(private val users: UserService) {

    @GetMapping
    fun list(): List<UserResponse> = users.list().map { it.toResponse() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody body: CreateUserRequest): UserResponse =
        users.create(name = body.name, email = body.email).toResponse()
}
