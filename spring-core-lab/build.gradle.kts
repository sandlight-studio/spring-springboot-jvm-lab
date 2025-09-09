plugins {
    kotlin("jvm")
    application
}

group = "studio.sandlight"
version = "0.0.1-SNAPSHOT"
description = "Spring Framework core (no Boot) examples"

kotlin { jvmToolchain(21) }

dependencies {
    implementation("org.springframework:spring-context:6.2.5")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.withType<Test> { useJUnitPlatform() }

application {
    mainClass.set("studio.sandlight.core.MainKt")
}
