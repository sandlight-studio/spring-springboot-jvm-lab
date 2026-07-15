package studio.sandlight.core

import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Scope
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import studio.sandlight.core.lifecycle.PrototypeThing

// No manual `open` needed: the kotlin-spring plugin opens @Configuration
// classes and their @Bean methods so CGLIB can subclass them.
@Configuration
@ComponentScan(basePackageClasses = [AppConfig::class])
@PropertySource("classpath:app.properties")
class AppConfig {
    @Bean
    fun message(): String = "Hello from Spring Context"

    companion object {
        // Enables @Value placeholders with @PropertySource
        // Static to avoid early @Configuration class instantiation (BeanFactoryPostProcessor lifecycle).
        @Bean
        @JvmStatic
        fun propertySourcesPlaceholderConfigurer(): PropertySourcesPlaceholderConfigurer =
            PropertySourcesPlaceholderConfigurer()
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun prototypeThing(): PrototypeThing = PrototypeThing()
}
