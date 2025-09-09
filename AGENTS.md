# Repository Guidelines

## Project Structure & Module Organization
- Multi-module Gradle (Kotlin DSL). Modules:
  - `boot-app` — Spring Boot web/JPA/validation.
  - `spring-core-lab` — plain Spring Framework examples.
  - `jvm-lab` — JVM/JFR/GC experiments.
- Code: `MODULE/src/main/kotlin/...`; tests: `MODULE/src/test/kotlin/...`.
- Config/assets per module under `src/main/resources/`.

## Build, Test, and Development Commands
- Root build: `./gradlew build` (all modules).
- Boot app: `./gradlew :boot-app:bootRun` (local server).
- Tests: `./gradlew :MODULE:test` (JUnit 5).
- Packaging: `./gradlew :boot-app:bootJar` (fat jar), optional `:boot-app:bootBuildImage`.

## Coding Style & Naming Conventions
- Kotlin 2.0, Java 21, Spring Boot 3.5. Use 4‑space indentation.
- Packages lowercase; classes `PascalCase`; functions/vars `camelCase`.
- Keep modules cohesive and layers thin (controller → service → repository).
- Favor immutability, data classes, and explicit nullability.

## Testing Guidelines
- JUnit 5; Spring Boot Test in `boot-app`, plain JUnit in others.
- Mirror package layout; name tests like `UserServiceTest.kt`.
- Prefer slice tests (`@WebMvcTest`, `@DataJpaTest`); use `@SpringBootTest` sparingly.

## Commit & Pull Request Guidelines
- Use imperative subjects (≤72 chars) and explain why not just what.
  - Example: "Add paging to UserController (closes #42)".
- PRs include: summary, rationale, test steps, and screenshots/curl when relevant.
- Require green CI and updated docs for behavior changes.

## Security & Configuration Tips
- No secrets in repo. Use env vars or external configs (`application-*.properties`).
- Use Spring profiles (`spring.profiles.active=dev|test|prod`).
- Restrict Actuator endpoints outside dev; default DB is H2 for local.
