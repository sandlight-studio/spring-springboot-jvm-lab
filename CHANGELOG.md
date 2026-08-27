# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.4] - 2026-08-27

### Changed

- Replace the MIT license with Apache-2.0, aligning with the other public repositories in the organization.
- Repository transferred from `Jimmy-Smo` to the `sandlight-studio` organization.

## [0.0.3] - 2026-07-15

### Added
- Add `lang-lab` Kotlin language features topic with 5-level content (data classes through coroutines)
- Add `lang-lab` reflection topic, split from basics into its own sub-package
- Add smoke tests for `lang-lab`, `jvm-lab`, and `spring-core-lab`
- Add `boot-app` service layer, request/response DTOs, and RFC 9457 problem-details error responses

### Changed
- Extract `buildSrc` convention plugins to centralize build logic across modules
- Align all modules on JUnit 6 (managed by the Spring Boot 4 BOM)
- Split `lang-lab` topics (strings, collections, concurrency, IO, reflection) into sub-packages with leveled teaching content
- Unify `lang-lab` topics behind a Topic/Level registry with a shared console, so usage text, dispatch, and smoke tests derive from one list

### Fixed
- Fix `jvm-lab` stack, string-intern, and volatile demos so each actually demonstrates its lesson

## [0.0.2] - 2026-02-05

### Added
- Add `boot-app` smoke tests for web + JPA behavior (controller, repository, integration-style tests)
- Introduce Gradle Version Catalog (`gradle/libs.versions.toml`) for centralized version management

### Changed
- Upgrade repo toolchain baseline to JDK 25
- Upgrade Gradle wrapper to 9.3.0
- Upgrade Kotlin to 2.3.0
- Upgrade Spring Boot to 4.0.2 (Spring Framework 7 generation)
- Upgrade `spring-core-lab` to Spring Framework 7.0.3

### Fixed
- Fix Boot 4 `CommandLineRunner` signature override in `DevDataLoader`
- Fix Spring Core lab `BeanFactoryPostProcessor` lifecycle warning by making `PropertySourcesPlaceholderConfigurer` static

## [0.0.1] - 2025-10-26

### Added
- Initial multi-module Kotlin + Spring learning monorepo
- `boot-app` module: Spring Boot 3.5.7 web application with actuator, JPA/H2, validation
- `spring-core-lab` module: Plain Spring Framework examples (ApplicationContext, beans, scopes, events)
- `jvm-lab` module: JVM performance experiments (allocations, GC, JFR)
- `lang-lab` module: Kotlin/Java language basics (strings, collections, concurrency, IO)
- Gradle 9.1.0 with Kotlin DSL build configuration and centralized plugin versions
- Kotlin 2.2.0 with JDK 21 toolchain support
- H2 in-memory database integration
- Spring Boot Actuator endpoints for observability
- CHANGELOG.md to track project changes
- Version badges in README.md for better visibility
- gradle.properties with configuration cache enabled for faster builds
- Build performance optimizations (parallel execution, caching, JVM tuning)
- Comprehensive README with usage examples
- CLAUDE.md for AI-assisted development guidance
- MIT License

[Unreleased]: https://github.com/sandlight-studio/spring-springboot-jvm-lab/compare/0.0.3...HEAD
[0.0.3]: https://github.com/sandlight-studio/spring-springboot-jvm-lab/compare/0.0.2...0.0.3
[0.0.2]: https://github.com/sandlight-studio/spring-springboot-jvm-lab/compare/0.0.1...0.0.2
[0.0.1]: https://github.com/sandlight-studio/spring-springboot-jvm-lab/releases/tag/0.0.1
