package studio.sandlight.core.events

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

data class GreetingCreatedEvent(val message: String)

@Component
class GreetingEventListener {
    @EventListener
    fun onGreeting(event: GreetingCreatedEvent) {
        println("[event] received -> ${event.message}")
    }
}

