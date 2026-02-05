package studio.sandlight.app.repo

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import studio.sandlight.app.domain.User

@SpringBootTest
class UserRepositoryTest(
    @Autowired private val repo: UserRepository,
) {

    @Test
    fun `existsByEmail returns false then true after insert`() {
        val email = "repo-test-${System.nanoTime()}@example.com"
        assertFalse(repo.existsByEmail(email))

        repo.save(User(name = "Test", email = email))

        assertTrue(repo.existsByEmail(email))
    }
}
