# spring-springboot-jvm-lab

A Kotlin + Spring multi-module monorepo for learning Spring Framework core (no Boot), Spring Boot, and JVM performance tooling (JFR, GC, benchmarking). Gradle (Kotlin DSL), JDK 21, Kotlin 2.0.x.

## Requirements
- JDK 21 on PATH
- Use the Gradle wrapper (`./gradlew`)

## Modules
- `boot-app` — Spring Boot 3 app (web, actuator, validation, JPA/H2).
- `spring-core-lab` — Plain Spring Framework (ApplicationContext, config, beans).
- `spring-core-lab` — Plain Spring Framework (ApplicationContext, @Bean, @Component, scopes, events).
- `jvm-lab` — JVM experiments (allocations, GC/JFR exercises).

## Quick Start
```bash
# Run Boot app
./gradlew :boot-app:bootRun
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/hello
curl http://localhost:8080/api/users
curl -X POST http://localhost:8080/api/users -H 'Content-Type: application/json' \
  -d '{"name":"Jane","email":"jane@example.com"}'

# Run Spring Core example
./gradlew :spring-core-lab:run --quiet                 # default
./gradlew :spring-core-lab:run --args="Alice" --quiet  # pass a name

# JVM lab examples
# Allocation demo for 10s (back-compat numeric arg)
./gradlew :jvm-lab:run --args=10 --quiet
# Or explicitly choose a demo:
./gradlew :jvm-lab:run --args="alloc 10" --quiet
./gradlew :jvm-lab:run --args=classloaders --quiet
./gradlew :jvm-lab:run --args="stack 2000" --quiet
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
