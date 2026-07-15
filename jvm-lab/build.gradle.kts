plugins {
    id("sandlight.kotlin-application-conventions")
}

description = "JVM experiments (GC, JFR, performance)"

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass.set("studio.sandlight.jvm.MainKt")
}
