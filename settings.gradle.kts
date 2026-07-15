rootProject.name = "spring-springboot-jvm-lab"

pluginManagement {
    repositories {
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        maven(url = "https://maven.aliyun.com/repository/public")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven(url = "https://maven.aliyun.com/repository/public")
        mavenCentral()
    }
}

include(
    "boot-app",
    "spring-core-lab",
    "jvm-lab",
    "lang-lab",
)
