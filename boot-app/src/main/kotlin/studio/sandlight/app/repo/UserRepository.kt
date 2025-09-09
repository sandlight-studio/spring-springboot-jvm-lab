package studio.sandlight.app.repo

import org.springframework.data.jpa.repository.JpaRepository
import studio.sandlight.app.domain.User

interface UserRepository : JpaRepository<User, Long> {
    fun existsByEmail(email: String): Boolean
}

