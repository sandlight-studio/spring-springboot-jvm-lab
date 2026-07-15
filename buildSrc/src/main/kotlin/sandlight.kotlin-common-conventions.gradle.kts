import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Shared Kotlin/JVM setup for every module. Modules apply this (directly or
// via sandlight.kotlin-application-conventions) instead of repeating the
// toolchain/test wiring; bumping the JDK now means editing one file.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "studio.sandlight"
version = "0.0.2"

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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
