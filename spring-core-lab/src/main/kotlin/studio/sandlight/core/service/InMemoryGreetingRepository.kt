package studio.sandlight.core.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

@Repository
class InMemoryGreetingRepository(
    @Value("\${app.greeting:Hello}") private val greeting: String,
) : GreetingRepository {
    override fun prefix(): String = greeting
}

