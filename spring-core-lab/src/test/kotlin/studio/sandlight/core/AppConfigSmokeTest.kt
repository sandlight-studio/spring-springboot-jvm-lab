package studio.sandlight.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import studio.sandlight.core.lifecycle.PrototypeThing
import studio.sandlight.core.service.GreetingService

/**
 * Boots the plain-Spring context the same way Main.kt does and checks the
 * core wiring: @Bean methods, component scanning, @Value placeholders, and
 * prototype scope. Also proves CGLIB can proxy AppConfig without manual
 * `open` (the kotlin-spring plugin opens it at compile time).
 */
class AppConfigSmokeTest {

    @Test
    fun `context starts and core beans resolve`() {
        AnnotationConfigApplicationContext(AppConfig::class.java).use { ctx ->
            assertEquals("Hello from Spring Context", ctx.getBean(String::class.java))

            val greeting = ctx.getBean(GreetingService::class.java).greet("Test")
            assertTrue(greeting.contains("Test")) { "unexpected greeting: $greeting" }

            val p1 = ctx.getBean(PrototypeThing::class.java)
            val p2 = ctx.getBean(PrototypeThing::class.java)
            assertNotEquals(p1.id, p2.id, "prototype scope must produce distinct instances")
        }
    }
}
