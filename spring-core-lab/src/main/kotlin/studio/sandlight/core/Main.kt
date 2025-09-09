package studio.sandlight.core

import org.springframework.context.annotation.AnnotationConfigApplicationContext

fun main() {
    AnnotationConfigApplicationContext(AppConfig::class.java).use { ctx ->
        val msg = ctx.getBean(String::class.java)
        println("Spring Core says: $msg")
    }
}

