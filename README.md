# TaskMaster: Collaborative Task Tracking System

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Java Version](https://img.shields.io/badge/Java-25-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-blue.svg)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

A high-performance, modular-monolith collaborative task management backend engineered with **Java 25**, **Spring Boot 4.x**, and **PostgreSQL 17**. Built with hexagonal architecture boundaries, real-time messaging, and pluggable Generative AI capabilities.

---

## 🚀 Key Architectural Capabilities

- **Hexagonal Modular Monolith**: Strictly decoupled bounded contexts (`user`, `task`, `team`, `collaboration`, `notification`, `ai`) enforced via ArchUnit tests.
- **Enterprise Security**: Asymmetric RS256 JWT access tokens with JWKS rotation and family-based refresh token replay defense.
- **Robust Persistence**: PostgreSQL 17 datastore with Flyway migrations, JSONB attributes, and generated `tsvector` full-text search.
- **Event-Driven Messaging**: Broker-agnostic event pipeline using Spring Cloud Stream with RabbitMQ binder (swappable to Kafka).
- **Bidirectional Real-Time Updates**: WebSocket with STOMP protocol and SockJS fallback for live notifications.
- **Universal Generative AI**: Universal OpenAI-compatible engine supporting Groq Cloud, Google Gemini (OpenAI endpoint), Ollama, vLLM, and enterprise AI Gateways with resilient heuristic fallback.
- **Production Observability**: Health probes, Micrometer metrics, and RFC 7807 `ProblemDetail` error handling.

---

## 🛠️ Technology Stack

| Layer | Technology |
|:---|:---|
| **Language** | Java 25 (LTS) |
| **Framework** | Spring Boot 4.x + Spring Security 6.x/7.x |
| **Database** | PostgreSQL 17 with Flyway Migrations |
| **Caching & Rate Limiting** | Redis 7 |
| **Messaging** | Spring Cloud Stream + RabbitMQ |
| **Real-time** | WebSocket + STOMP (SockJS) |
| **Object Storage** | MinIO (local S3-compatible) / AWS S3 |
| **AI Integration** | Universal OpenAI-Compatible Client (Groq, Gemini, Ollama, AI Gateways) |
| **Testing** | JUnit 5, Testcontainers, ArchUnit, AssertJ |
| **API Documentation** | OpenAPI 3.1 & Swagger UI |

---

## 📋 Prerequisites

- **Java JDK 25** (e.g. GraalVM / Eclipse Temurin / OpenJDK 25)
- **Docker & Docker Compose** (for PostgreSQL, Redis, RabbitMQ, MinIO)
- **Gradle 8.x+** (or use the included `./gradlew`)

---

## ⚡ Quick Start

### 1. Clone the repository
```bash
git clone https://github.com/shashankchandel/task-master.git
cd task-master
```

### 2. Start Infrastructure Dependencies
Spin up PostgreSQL 17, Redis 7, RabbitMQ, and MinIO:
```bash
docker compose up -d
```

Verify service health:
```bash
docker compose ps
```

### 3. Build & Run the Application
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The application starts on `http://localhost:8080`.

---

## 📖 API Documentation & Endpoints

TaskMaster exposes a versioned, RESTful API secured by asymmetric RS256 JWTs with RFC 7807 error envelopes.

- 📘 **Complete Specification & Payloads**: See [docs/api-specification.md](./docs/api-specification.md)
- 🖥️ **Interactive Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- ⚙️ **OpenAPI JSON Schema**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### API Module Overview

| Module | Base Path | Key Capabilities | Auth Required |
|:---|:---|:---|:---|
| **Authentication** | `/api/v1/auth` | User registration, login, refresh token rotation, logout, and public JWKS keys | No (Rate Limited) |
| **User Profile** | `/api/v1/users` | Profile retrieval and display name/avatar updates | Yes (Bearer JWT) |
| **Task Management** | `/api/v1/tasks` | CRUD, lifecycle state transitions, assignee assignment, JPA criteria filtering, full-text search, and soft deletion | Yes (Bearer JWT) |
| **Team Workspaces** | `/api/v1/teams` | Workspace creation, member governance, secure invite code generation/joining, and role management | Yes (Bearer JWT) |
| **Task Comments** | `/api/v1/tasks/{taskId}/comments` | Hierarchical threaded task comments, nested replies, editing, and soft deletion | Yes (Bearer JWT) |
| **Task Attachments** | `/api/v1/tasks/{taskId}/attachments` | Multipart file upload (Max 10MB), S3/MinIO storage, pre-signed download URLs, and deletion | Yes (Bearer JWT) |
| **Notifications** | `/api/v1/notifications` | Real-time STOMP push (`/ws`), unread count badge, mark as read, and persistent notification center | Yes (Bearer JWT) |
| **AI Assistant** | `/api/v1/ai` | Pluggable Generative AI (Gemini/Groq/Ollama) for description synthesis, summarization, priority, and duplicate detection | Yes (Bearer JWT) |

---

## 🧪 Testing & Code Quality

Run the test suite including ArchUnit boundary verifications:
```bash
./gradlew test
```

Run static analysis and style checks:
```bash
./gradlew check
```

---

## 🗺️ Product Roadmap

Track implementation progress across release phases:

- ✅ **Phase 1: Foundation & Project Setup**
- ✅ **Phase 2: User Authentication & Authorization**
- ✅ **Phase 3: Task Management**
- ✅ **Phase 4: Team Collaboration**
- ✅ **Phase 5: Real-time Notifications & AI Integration**
- ⬜ **Phase 6: Advanced Search & Analytics**
- ⬜ **Phase 7: Performance & Reliability**
- ⬜ **Phase 8: DevOps & Cloud Deployment**
- ⬜ **Phase 9: Extended Platform Capabilities**
- ⬜ **Phase 10: Modern Collaborative Web Application (Frontend)**

For detailed milestone breakdowns, see [docs/ROADMAP.md](./docs/ROADMAP.md).

---

## 🤝 Contributing & Guidelines

Please read [CONTRIBUTING.md](./CONTRIBUTING.md) for details on our branch naming conventions, conventional commit standards, and pull request process.

Architecture Decision Records are available in [docs/adr/](./docs/adr/).
For complete system design and C4 diagrams, see [docs/architecture.md](./docs/architecture.md).

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.
