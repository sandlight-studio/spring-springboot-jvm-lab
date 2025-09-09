package studio.sandlight.app.bootstrap

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import studio.sandlight.app.domain.User
import studio.sandlight.app.repo.UserRepository

@Component
@Profile("dev")
class DevDataLoader(private val repo: UserRepository) : CommandLineRunner {
    override fun run(vararg args: String?) {
        if (repo.count() == 0L) {
            repo.saveAll(
                listOf(
                    User(name = "Alice", email = "alice@example.com"),
                    User(name = "Bob", email = "bob@example.com"),
                )
            )
        }
    }
}

