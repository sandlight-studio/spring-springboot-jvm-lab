package studio.sandlight.app.web

import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import studio.sandlight.app.config.AppProps

@RestController
@RequestMapping("/api")
class HelloController(
    private val props: AppProps,
    private val env: Environment,
) {
    @GetMapping("/hello")
    fun hello(): Map<String, Any?> = mapOf(
        "message" to "${props.greeting} from Spring Boot",
        "featureFlag" to props.featureFlag,
        "profiles" to env.activeProfiles.toList(),
        "app" to env.getProperty("spring.application.name"),
    )
}

