import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "studio.sandlight"
version = "0.0.1-SNAPSHOT"
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
    testImplementation(libs.junit.jupiter)
}

tasks.withType<Test> { useJUnitPlatform() }

application {
    mainClass.set("studio.sandlight.lang.MainKt")
}
