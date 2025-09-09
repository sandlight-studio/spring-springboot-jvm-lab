# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test                                    # all modules
./gradlew :boot-app:test                         # single module
./gradlew :spring-core-lab:test :jvm-lab:test   # multiple modules

# Run applications
./gradlew :boot-app:bootRun                      # Spring Boot app (port 8080)
./gradlew :spring-core-lab:run --quiet           # Spring Core demo
./gradlew :jvm-lab:run --args="alloc 10" --quiet # JVM demos
./gradlew :lang-lab:run --args=strings --quiet   # Language demos

# Build specific artifacts
./gradlew :boot-app:bootJar                      # Spring Boot JAR
```

## Architecture

This is a Kotlin + Spring multi-module monorepo with 4 learning modules:

- **boot-app**: Spring Boot 3 web application with actuator, JPA/H2, validation. Main class: `studio.sandlight.app.AppApplication`
- **spring-core-lab**: Plain Spring Framework examples (ApplicationContext, @Bean, @Component, scopes, events). Main class: `studio.sandlight.core.MainKt` 
- **jvm-lab**: JVM performance experiments (allocations, GC, JFR). Main class: `studio.sandlight.jvm.MainKt`
- **lang-lab**: Kotlin/Java language basics (strings, collections, concurrency, IO). Main class: `studio.sandlight.lang.MainKt`

### Key Technical Details

- **JDK 21** with Kotlin 2.0.x
- **Gradle Kotlin DSL** with centralized plugin versions in root `build.gradle.kts`
- **Package structure**: All under `studio.sandlight.*` namespace
- **Testing**: JUnit 5 across all modules
- **Spring Boot**: Uses H2 in-memory database, profiles supported (`dev` profile available)

### Development Patterns

- Each non-Boot module has an `application` plugin with a main class
- Boot app follows standard Spring Boot structure: controllers in `web/`, domain in `domain/`, repos in `repo/`
- JVM and lang labs use command-line arguments to select different demos
- Use `--quiet` flag when running demos to reduce Gradle output noise

## Spring Boot App Endpoints

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/hello
curl http://localhost:8080/api/users
curl -X POST http://localhost:8080/api/users -H 'Content-Type: application/json' -d '{"name":"Jane","email":"jane@example.com"}'
```

## JVM Observability

For JFR/GC monitoring:
```bash
JAVA_TOOL_OPTIONS="-Xlog:gc*:time -XX:StartFlightRecording=filename=app.jfr,duration=60s" ./gradlew :boot-app:bootRun
```