package studio.sandlight.app.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProps(
    var greeting: String = "Hello",
    var featureFlag: Boolean = false,
)

