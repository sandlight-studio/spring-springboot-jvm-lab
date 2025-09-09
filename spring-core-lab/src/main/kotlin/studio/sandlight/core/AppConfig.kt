package studio.sandlight.core

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfig {
    @Bean
    fun message(): String = "Hello from Spring Context"
}

