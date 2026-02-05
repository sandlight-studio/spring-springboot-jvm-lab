group = "studio.sandlight"
version = "0.0.2"
description = "Spring + Kotlin multi-module learning monorepo"

plugins {
    // Centralize plugin versions via Version Catalog; subprojects apply without versions.
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

allprojects {
    repositories {
        maven(url = "https://maven.aliyun.com/repository/public")
        mavenCentral()
    }
}
