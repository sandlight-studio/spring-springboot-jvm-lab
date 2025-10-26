# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Upgraded Kotlin from 2.0.21 to 2.2.0
- Upgraded Spring Boot from 3.5.5 to 3.5.7
- Upgraded Gradle from 8.14.3 to 9.1.0

### Added
- CHANGELOG.md to track project changes
- Version badges in README.md for better visibility
- gradle.properties with configuration cache enabled for faster builds
- Build performance optimizations (parallel execution, caching, JVM tuning)

## [0.0.1-SNAPSHOT] - 2025-10-26

### Added
- Initial multi-module Kotlin + Spring learning monorepo
- `boot-app` module: Spring Boot 3 web application with actuator, JPA/H2, validation
- `spring-core-lab` module: Plain Spring Framework examples (ApplicationContext, beans, scopes, events)
- `jvm-lab` module: JVM performance experiments (allocations, GC, JFR)
- `lang-lab` module: Kotlin/Java language basics (strings, collections, concurrency, IO)
- Gradle Kotlin DSL build configuration with centralized plugin versions
- JDK 21 toolchain support
- H2 in-memory database integration
- Spring Boot Actuator endpoints for observability
- Comprehensive README with usage examples
- CLAUDE.md for AI-assisted development guidance
- MIT License

[Unreleased]: https://github.com/sandlight-studio/spring-springboot-jvm-lab/compare/v0.0.1...HEAD
[0.0.1-SNAPSHOT]: https://github.com/sandlight-studio/spring-springboot-jvm-lab/releases/tag/v0.0.1
