plugins {
    id("sandlight.kotlin-application-conventions")
    // kotlin-spring's allOpen support opens @Configuration/@Component classes
    // and @Bean methods automatically, so AppConfig needs no manual `open`.
    id("org.jetbrains.kotlin.plugin.spring")
}

description = "Spring Framework core (no Boot) examples"

dependencies {
    implementation(libs.spring.context)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass.set("studio.sandlight.core.MainKt")
}
