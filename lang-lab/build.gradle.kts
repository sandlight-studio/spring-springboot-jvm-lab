plugins {
    id("sandlight.kotlin-application-conventions")
}

description = "Kotlin/Java basics: strings, collections, concurrency, IO"

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass.set("studio.sandlight.lang.MainKt")
    // StringFundamentals/StringPerformance reflect into String.value/coder.
    applicationDefaultJvmArgs = listOf("--add-opens=java.base/java.lang=ALL-UNNAMED")
}

tasks.test {
    // Same opens for the smoke tests that run those demos.
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
}
