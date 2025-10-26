# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/sandlight-studio/spring-springboot-jvm-lab/compare/v0.0.1...HEAD
[0.0.1]: https://github.com/sandlight-studio/spring-springboot-jvm-lab/releases/tag/v0.0.1
