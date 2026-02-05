import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "studio.sandlight"
version = "0.0.2"
description = "JVM experiments (GC, JFR, performance)"

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
    mainClass.set("studio.sandlight.jvm.MainKt")
}
