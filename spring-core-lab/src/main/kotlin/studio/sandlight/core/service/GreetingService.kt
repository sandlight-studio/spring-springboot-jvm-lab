package studio.sandlight.core.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import studio.sandlight.core.events.GreetingCreatedEvent

@Service
class GreetingService(
    private val repository: GreetingRepository,
    private val events: ApplicationEventPublisher,
    @Value("\${app.name:core-app}") private val appName: String,
) {
    fun greet(who: String): String {
        val message = "${repository.prefix()}, $who! ($appName)"
        events.publishEvent(GreetingCreatedEvent(message))
        return message
    }
}

interface GreetingRepository {
    fun prefix(): String
}

