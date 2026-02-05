# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/sandlight-studio/spring-springboot-jvm-lab/compare/v0.0.2...HEAD
[0.0.2]: https://github.com/sandlight-studio/spring-springboot-jvm-lab/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/sandlight-studio/spring-springboot-jvm-lab/releases/tag/v0.0.1
