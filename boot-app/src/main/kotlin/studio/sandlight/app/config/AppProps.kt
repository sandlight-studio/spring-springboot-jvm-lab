package studio.sandlight.app.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Immutable constructor binding: Boot binds `app.*` properties through the
 * constructor, so `val` works and the object can't be mutated after startup
 * (the older JavaBean style needed `var` + setters).
 */
@ConfigurationProperties(prefix = "app")
data class AppProps(
    val greeting: String = "Hello",
    val featureFlag: Boolean = false,
)
