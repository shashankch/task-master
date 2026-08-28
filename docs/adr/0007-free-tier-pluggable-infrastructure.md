# ADR 0007: Pluggable Free-Tier & Open-Source First Infrastructure Strategy

## Status
Accepted

## Context
Deploying, developing, and evaluating modern cloud-native systems requires minimizing operational friction, licensing barriers, and proprietary infrastructure dependencies while preserving seamless forward compatibility with managed enterprise cloud platforms.

Developers and operators need:
1. **Zero-Cost Local Development Parity**: 100% functional local execution without requiring paid cloud accounts, proprietary APIs, or active credit card billing.
2. **Open-Source Defaults**: Industry-standard open-source components for storage, relational data, caching, telemetry, and artificial intelligence.
3. **Frictionless Enterprise Upgrades**: One-line configuration switches to upgrade to high-availability managed cloud equivalents without code changes.

## Decision
We adopt a **Pluggable Free-Tier & Open-Source First Infrastructure Strategy** across all core platform capabilities:

| Domain | Open-Source / Free-Tier Default | Enterprise / Managed Equivalent | Switch Mechanism |
|:---|:---|:---|:---|
| **AI Engine** | Groq Cloud (`llama-3.3-70b` free tier), Google Gemini (`gemini-2.5-flash` free tier), Local Ollama (`llama3.2` 100% offline) | OpenAI GPT-4o, Anthropic Claude, Enterprise AI Gateways (LiteLLM/Portkey) | Environment variables (`AI_BASE_URL`, `AI_API_KEY`, `AI_MODEL`) |
| **Object Storage** | MinIO (100% open-source S3 API) via Docker Compose | AWS S3, Cloudflare R2, Google Cloud Storage | Environment variables (`AWS_S3_ENDPOINT`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`) |
| **Relational Database** | PostgreSQL 17 (open-source) via Docker Compose / Neon Serverless (free tier) | AWS Aurora PostgreSQL, Google Cloud SQL, Azure Database for PostgreSQL | JDBC URL & Spring profile (`application-prod.yml`) |
| **Cache & Limiting** | Redis 7 (open-source) via Docker Compose / Upstash Redis (free tier) | AWS ElastiCache for Redis, Redis Enterprise Cloud | Connection host & password environment variables |
| **Observability** | OpenTelemetry + Jaeger / Prometheus / Grafana Loki (open-source) | Grafana Cloud, Datadog, Dynatrace, New Relic | Standard OTLP endpoint (`OTEL_EXPORTER_OTLP_ENDPOINT`) |
| **API Contract** | OpenAPI 3.1 static YAML & Swagger UI (open-source) | Postman Enterprise, Stoplight, SwaggerHub | Standard `docs/api/openapi.yaml` |

## Consequences
### Positive
- **Instant Developer Onboarding**: A complete development environment starts with a single command (`docker compose up -d && ./gradlew bootRun`) with zero external API dependencies.
- **Predictable Cost Control**: Development, staging, and continuous integration pipelines operate at zero cloud infrastructure cost.
- **Portability**: The application runs identically on bare metal, local laptops, Kubernetes, AWS, GCP, or Azure.
