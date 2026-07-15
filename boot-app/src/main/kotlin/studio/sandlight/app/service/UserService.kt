package studio.sandlight.app.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import studio.sandlight.app.domain.User
import studio.sandlight.app.repo.UserRepository

/**
 * Business rules and transaction boundaries live here, not in the controller:
 * the web layer translates HTTP, the service decides what is allowed.
 */
@Service
class UserService(private val repo: UserRepository) {

    fun list(): List<User> = repo.findAll()

    @Transactional
    fun create(name: String, email: String): User {
        if (repo.existsByEmail(email)) throw DuplicateEmailException(email)
        return repo.save(User(name = name, email = email))
    }
}

/** Domain-specific failure; mapped to 409 Conflict in GlobalExceptionHandler. */
class DuplicateEmailException(email: String) : RuntimeException("email already exists: $email")
