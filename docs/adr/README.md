# TaskMaster — Architecture Decision Records (ADRs)

This directory documents all significant architectural and technical decisions made for the TaskMaster platform. Each record follows the standard Michael Nygard ADR format (Context, Decision, Consequences).

---

## Index of Architectural Decisions

| ADR | Title | Approval Date | Status | Summary |
| :--- | :--- | :--- | :--- | :--- |
| [**0001**](0001-modular-monolith.md) | Modular Monolith Architecture | 2026-08-20 | Accepted | Modular monolith with clean hexagonal package boundaries per business module over microservices. |
| [**0002**](0002-postgresql.md) | PostgreSQL & Flyway Strategy | 2026-08-20 | Accepted | PostgreSQL 17 relational datastore with version-controlled Flyway migrations, tsvector FTS, and JSONB. |
| [**0003**](0003-hexagonal-architecture.md) | Hexagonal Architecture Boundaries | 2026-08-20 | Accepted | Ports & Adapters architecture within each domain module enforced at compile time by ArchUnit. |
| [**0004**](0004-spring-cloud-stream.md) | Event-Driven Architecture with Spring Cloud Stream | 2026-08-20 | Accepted | Broker-agnostic messaging using Spring Cloud Stream binders (RabbitMQ in dev, swappable to Kafka). |
| [**0005**](0005-ai-provider-strategy.md) | Universal OpenAI-Compatible AI Strategy | 2026-08-27 | Accepted | Universal `/chat/completions` REST client swappable across Groq, Gemini, Ollama, and AI Gateways. |
| [**0006**](0006-opentelemetry-vendor-neutrality.md) | OpenTelemetry (OTel) Standard for Observability | 2026-08-29 | Accepted | Vendor-neutral distributed tracing via standard OTLP HTTP exporter and W3C TraceContext propagation. |
| [**0007**](0007-free-tier-pluggable-infrastructure.md) | Pluggable Free-Tier & Open-Source First Strategy | 2026-08-29 | Accepted | Zero-cost local development defaults (Docker/MinIO/Jaeger) with 1-line environment variable cloud upgrade. |
| [**0008**](0008-multi-platform-cloud-paas-deployment-strategy.md) | Multi-Platform Cloud, PaaS & Container Deployment Strategy | 2026-09-03 | Accepted | Comprehensive multi-target deployment matrix covering Self-Hosting, PaaS (Railway/Render), K8s, and Cloud. |
| [**0009**](0009-multi-tenant-task-authorization-hardening.md) | Multi-Tenant Task Authorization Hardening | 2026-09-08 | Accepted | Scoped task search queries, personal task IDOR defense, and negative authorization testing. |
