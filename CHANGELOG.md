# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [0.3.0] - 2026-08-21

### Added
- Task management domain aggregate, state machine (`OPEN`, `IN_PROGRESS`, `REVIEW`, `COMPLETED`, `ARCHIVED`), and priority levels (`LOW`, `MEDIUM`, `HIGH`, `URGENT`).
- Task CRUD REST endpoints (`/api/v1/tasks`, `/api/v1/tasks/{id}`).
- Task status transitions with state machine validation (`/api/v1/tasks/{id}/status`).
- Task assignment and unassignment (`/api/v1/tasks/{id}/assign`).
- Dynamic search and multi-field filtering (`status`, `priority`, `assigneeId`, `creatorId`, `teamId`, `label`, date ranges, and keyword search).
- PostgreSQL full-text search `tsvector` generated column and GIN index (`V4__create_tasks_table.sql`).
- Task labels collection support (`V5__create_task_labels_table.sql`).
- Optimistic locking via JPA `@Version` on Task aggregate.
- Soft deletion support with `deleted_at` timestamp filtering.
- Domain event publishing for task lifecycle (`TaskCreatedEvent`, `TaskUpdatedEvent`, `TaskStatusChangedEvent`, `TaskAssignedEvent`, `TaskDeletedEvent`).
- Complete test coverage across domain model, service layer, MockMvc controllers, and end-to-end integration flows.

---

## [0.2.0] - 2026-08-20

### Added
- User registration and authentication endpoints (`/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/logout`).
- Asymmetric RS256 JWT access token generation and public JWKS endpoint (`/.well-known/jwks.json`).
- Refresh token rotation with family-based token replay attack detection and automatic family revocation.
- Spring Security OAuth2 Resource Server integration with role-based authorities mapping.
- User profile management endpoints (`/api/v1/users/me` [GET, PUT]).
- Redis-backed distributed sliding-window rate limiting with in-memory fallback on authentication endpoints.
- Database migrations for `users` (`V2`) and `refresh_tokens` (`V3`) tables with foreign keys and indexes.
- Baseline GitHub Actions CI/CD workflow (`.github/workflows/ci.yml`) for automated builds, linting, and tests.
- Comprehensive unit and integration test suite covering end-to-end authentication lifecycle.

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
