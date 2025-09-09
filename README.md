# spring-springboot-jvm-demo

A Kotlin + Spring multi-module monorepo for learning Spring Framework core (no Boot), Spring Boot, and JVM performance tooling (JFR, GC, benchmarking). Gradle (Kotlin DSL), JDK 21, Kotlin 2.0.x.

## Requirements
- JDK 21 on PATH
- Use the Gradle wrapper (`./gradlew`)

## Modules
- `boot-app` — Spring Boot 3 app (web, actuator, validation, JPA/H2).
- `spring-core-lab` — Plain Spring Framework (ApplicationContext, config, beans).
- `jvm-lab` — JVM experiments (allocations, GC/JFR exercises).

## Quick Start
```bash
# Run Boot app
./gradlew :boot-app:bootRun
curl http://localhost:8080/actuator/health

# Run Spring Core example
./gradlew :spring-core-lab:run --quiet  # or run Main.kt from IDE

# Run JVM allocation demo for 10s
./gradlew :jvm-lab:run --args=10 --quiet
```

## Build & Test
```bash
./gradlew build       # build all modules
./gradlew :boot-app:bootJar
./gradlew :spring-core-lab:test :jvm-lab:test
```

## Configuration
- Boot uses in-memory H2 by default. Profiles supported:
```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew :boot-app:bootRun
```
- Do not commit secrets. Prefer env vars or external config.

## JVM & Observability
- Actuator exposed for local use: `/actuator/health`.
- Example local tuning:
```bash
JAVA_TOOL_OPTIONS="-Xlog:gc*:time -XX:StartFlightRecording=filename=app.jfr,duration=60s" \
  ./gradlew :boot-app:bootRun
```

## Contributing
- Follow Kotlin conventions (4 spaces). Clear, imperative commits. Add tests when changing behavior.

## License
See `LICENSE`.
