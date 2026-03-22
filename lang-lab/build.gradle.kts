import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "studio.sandlight"
version = "0.0.2"
description = "Kotlin/Java basics: strings, collections, concurrency, IO"

kotlin {
    jvmToolchain(25)
    compilerOptions {
        // Keep compilation consistent across Java/Kotlin tasks.
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit.jupiter)
}

tasks.withType<Test> { useJUnitPlatform() }

application {
    mainClass.set("studio.sandlight.lang.MainKt")
    applicationDefaultJvmArgs = listOf("--add-opens=java.base/java.lang=ALL-UNNAMED")
}
