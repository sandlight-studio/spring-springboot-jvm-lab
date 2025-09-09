package studio.sandlight.core

import org.springframework.context.annotation.AnnotationConfigApplicationContext
import studio.sandlight.core.lifecycle.PrototypeThing
import studio.sandlight.core.service.GreetingService

fun main(args: Array<String>) {
    AnnotationConfigApplicationContext(AppConfig::class.java).use { ctx ->
        // Basic bean lookup
        val msg = ctx.getBean(String::class.java)
        println("[bean] message -> $msg")

        // Constructor injection + @Value + events
        val greeter = ctx.getBean(GreetingService::class.java)
        val name = args.firstOrNull() ?: "World"
        println("[service] ${greeter.greet(name)}")

        // Scope demo: prototype vs singleton
        val p1 = ctx.getBean(PrototypeThing::class.java)
        val p2 = ctx.getBean(PrototypeThing::class.java)
        println("[scope] prototype instances differ: ${p1.id} != ${p2.id}")
    }
}
