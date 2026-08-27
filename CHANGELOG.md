# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [1.0.0] - 2026-08-23

### Added
- Real-time notification system with WebSocket and STOMP message broker (`/ws`), JWT channel handshake authentication, and private user destination queues (`/user/queue/notifications`).
- Persistent Notification Center REST API:
  - `GET /api/v1/notifications`: Paginated list of notifications with unread filtering.
  - `GET /api/v1/notifications/unread-count`: Fast badge counter for unread notifications.
  - `PATCH /api/v1/notifications/{id}/read`: Mark single notification as read.
  - `PATCH /api/v1/notifications/read-all`: Bulk mark all notifications as read for authenticated user.
- Event-driven notification listener translating domain events (`TaskAssignedEvent`, `TaskCommentCreatedEvent`, `TaskStatusChangedEvent`, `TeamMemberJoinedEvent`) into persistent and real-time push alerts.
- Database migration `V11__create_notifications_table.sql` with partial indexing on unread notifications and foreign keys.
- Universal OpenAI-Compatible Generative AI Assistant module (`/api/v1/ai`):
  - Standard `POST /chat/completions` REST client compatible with Groq Cloud (`llama-3.3-70b-versatile`), Google Gemini (OpenAI endpoint), local Ollama (`llama3.2`), and enterprise AI Gateways with resilient context-aware heuristic fallback.
  - `POST /api/v1/ai/generate-description`: Synthesize comprehensive Markdown task descriptions and checkbox acceptance criteria from brief user prompts.
  - `POST /api/v1/ai/summarize-task/{taskId}`: Executive summary distilling task metadata and all threaded comments into key takeaways and action items.
  - `POST /api/v1/ai/suggest-priority`: Intelligent priority recommendation (`LOW`, `MEDIUM`, `HIGH`, `URGENT`) with confidence score and reasoning.
  - `POST /api/v1/ai/detect-duplicates`: Semantic duplicate task detection across workspace tasks.
  - `POST /api/v1/ai/suggest-labels`: Smart technical categorization tag suggestions based on task context.
- Full test coverage for notification lifecycle and AI assistant workflows (total tests increased to 99 passing tests).

---

## [0.4.0] - 2026-08-22

### Added
- Team workspace management module with workspace creation, invite code generation, invite-based joining, and role governance (`OWNER`, `ADMIN`, `MEMBER`).
- Team REST endpoints (`/api/v1/teams`, `/api/v1/teams/{id}`, `/api/v1/teams/join`, `/api/v1/teams/{id}/invite-code/regenerate`, `/api/v1/teams/{id}/members`, `/api/v1/teams/{id}/members/{userId}/role`).
- Threaded hierarchical task comments with recursive replies, author editing, and soft deletion (`/api/v1/tasks/{taskId}/comments`, `/api/v1/comments/{id}`).
- Task file attachments module supporting multipart uploads (up to 10MB), S3/MinIO compatible storage, and pre-signed download URLs (`/api/v1/tasks/{taskId}/attachments`, `/api/v1/attachments/{id}`).
- Database migrations:
  - `V6__create_teams_table.sql`: `teams` workspace table with unique invite codes and owner constraints.
  - `V7__create_team_members_table.sql`: `team_members` membership table with role check constraints.
  - `V8__add_team_fk_to_tasks.sql`: Foreign key constraint linking tasks to team workspaces.
  - `V9__create_task_comments_table.sql`: `task_comments` table with hierarchical parent-child relationships and soft deletion.
  - `V10__create_task_attachments_table.sql`: `task_attachments` table tracking file metadata and object storage keys.
- Domain event publishing for team and collaboration lifecycles (`TeamCreatedEvent`, `TeamMemberJoinedEvent`, `TeamMemberRoleUpdatedEvent`, `TeamMemberRemovedEvent`, `TeamDeletedEvent`, `TaskCommentCreatedEvent`, `TaskCommentUpdatedEvent`, `TaskCommentDeletedEvent`, `TaskAttachmentUploadedEvent`, `TaskAttachmentDeletedEvent`).
- Comprehensive unit, MockMvc, and end-to-end integration test suites across team workspaces and collaboration flows (73 total tests passing).
- Production-grade REST API specification document in `docs/api-specification.md`.

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
