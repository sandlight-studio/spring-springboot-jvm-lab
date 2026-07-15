package studio.sandlight.app.repo

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import studio.sandlight.app.domain.User

/**
 * @DataJpaTest boots only the JPA slice (entities, repositories, an embedded
 * H2) instead of the whole application, and wraps each test in a rolled-back
 * transaction — so a fixed email needs no uniqueness trick.
 */
@DataJpaTest
class UserRepositoryTest(
    @param:Autowired private val repo: UserRepository,
) {

    @Test
    fun `existsByEmail returns false then true after insert`() {
        val email = "repo-test@example.com"
        assertFalse(repo.existsByEmail(email))

        repo.save(User(name = "Test", email = email))

        assertTrue(repo.existsByEmail(email))
    }
}
