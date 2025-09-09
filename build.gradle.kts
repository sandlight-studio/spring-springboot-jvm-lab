group = "studio.sandlight"
version = "0.0.1-SNAPSHOT"
description = "Spring + Kotlin multi-module learning monorepo"

plugins {
    // Centralize plugin versions; subprojects apply without versions
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.spring") version "2.0.21" apply false
    kotlin("plugin.jpa") version "2.0.21" apply false
    id("org.springframework.boot") version "3.5.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    repositories { mavenCentral() }
}
