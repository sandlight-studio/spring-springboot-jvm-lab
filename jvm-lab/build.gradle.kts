plugins {
    kotlin("jvm")
    application
}

group = "studio.sandlight"
version = "0.0.1-SNAPSHOT"
description = "JVM experiments (GC, JFR, performance)"

kotlin { jvmToolchain(21) }

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.withType<Test> { useJUnitPlatform() }

application {
    mainClass.set("studio.sandlight.jvm.MainKt")
}
