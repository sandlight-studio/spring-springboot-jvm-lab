plugins {
    kotlin("jvm")
    application
}

group = "studio.sandlight"
version = "0.0.1-SNAPSHOT"
description = "Kotlin/Java basics: strings, collections, concurrency, IO"

kotlin { jvmToolchain(21) }

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.withType<Test> { useJUnitPlatform() }

application {
    mainClass.set("studio.sandlight.lang.MainKt")
}

