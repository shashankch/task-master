# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [0.1.0] - 2026-08-20

### Added
- Modular monolith architectural skeleton using Spring Boot 4.x and Java 25.
- Multi-environment application configuration profiles (`dev`, `test`, `prod`).
- Multi-provider AI profile configurations (`ai-gemini`, `ai-groq`, `ai-ollama`).
- Docker Compose multi-service local environment (PostgreSQL 17, Redis 7, RabbitMQ, MinIO).
- Shared infrastructure module with RFC 7807 `ProblemDetail` global exception handler.
- JPA Auditing configuration with UUID-based `BaseEntity`.
- Flyway database migration baseline setup.
- Architecture enforcement rules using ArchUnit.
- OpenAPI 3.1 / Swagger UI API documentation integration.
- Standard project guidelines: `.editorconfig`, Checkstyle, `CONTRIBUTING.md`, Architecture Decision Records (ADRs).
