# TaskMaster Product Roadmap

A trackable overview of delivery milestones and planned capabilities for the TaskMaster collaborative platform.

---

## Core Delivery

- ✅ **Phase 1: Foundation & Project Setup**
  - Modular monolith project initialization with Java 25 & Spring Boot 4.x
  - Multi-profile environment configuration & container orchestration
  - Shared domain abstractions, auditing, and global exception infrastructure
  - Architectural constraints enforcement with ArchUnit
  - API documentation baseline with OpenAPI 3.1 / Swagger UI

- ✅ **Phase 2: User Authentication & Authorization**
  - User registration, login, and profile lifecycle management
  - Asymmetric RS256 JWT access tokens with JWKS rotation endpoint
  - Refresh token rotation with family-based replay attack mitigation
  - Role-based method-level security (`@PreAuthorize`)
  - Redis sliding window rate limiting on auth endpoints
  - Baseline GitHub Actions CI/CD automated workflow

- ✅ **Phase 3: Task Management**
  - Full CRUD operations with soft deletion and optimistic locking
  - Status state transitions (`OPEN` → `IN_PROGRESS` → `REVIEW` → `COMPLETED` → `ARCHIVED`)
  - Multi-dimensional JPA criteria filtering, multi-field sorting, and cursor pagination
  - PostgreSQL full-text search (`tsvector` & GIN index)
  - Domain event publishing for lifecycle triggers

- ✅ **Phase 4: Team Collaboration**
  - Team & project workspace creation with role-based member governance
  - Secure invite code generation and onboarding workflow
  - Team-scoped task querying and authorization gates
  - Threaded hierarchical task comments
  - Multipart file attachments with MinIO/S3 and pre-signed download URLs

- ✅ **Phase 5: Real-time Notifications & AI Integration**
  - Event-driven notifications pipeline via domain event listeners
  - Bidirectional real-time notification push using WebSocket & STOMP with JWT handshake
  - Pluggable Generative AI engine (Google Gemini, Groq, local Ollama)
  - Automated task description synthesis and executive comment summarization
  - Smart priority recommendations, duplicate task detection, and automatic tagging

- ✅ **Phase 6: Production Hardening, Security Remediation & Open Standards**
  - Persistent PEM-encoded RSA key pair for zero-session-loss restarts and multi-replica consistency
  - Environment-driven CORS origin allowlist eliminating wildcard credential exposure
  - Real AWS SDK v2 S3/MinIO durable file storage adapter with pre-signed URL offloading
  - Team-scoped authorization checks preventing IDOR vulnerabilities across task operations
  - Vendor-neutral OpenTelemetry (OTel) distributed tracing via standard OTLP protocol
  - Validated OpenAPI 3.1 YAML specification (`docs/api/openapi.yaml`) with CI export verification
  - Bounded Caffeine LRU cache fallback for sliding-window rate limiter
  - Java 25 compiler bytecode release alignment and expanded ArchUnit regression rule suite
  - Hibernate `@SQLRestriction` for automatic soft-delete query protection

---

## Advanced Horizons

- ⬜ **Phase 7: Advanced Search & Analytics**
  - Dedicated Elasticsearch cluster synchronization
  - Fuzzy typo-tolerant search and type-ahead auto-completion
  - Team velocity, cycle time, and workload distribution analytics
  - Event-sourced audit trail with full historical replay

- ⬜ **Phase 8: Performance & Reliability**
  - Distributed Redis rate limiting with atomic Lua execution
  - Multi-tier caching with declarative invalidation
  - Circuit breaking and fault tolerance via Resilience4j
  - Idempotency key tracking for non-idempotent mutations

- ⬜ **Phase 9: DevOps, Cloud & Multi-Platform Deployment**
  - Multi-stage minimal containerization with Distroless / Eclipse Temurin Java 25
  - Self-hosting & local orchestration with single-command Docker Compose stack
  - 1-Click PaaS deployment blueprints (`railway.json` and `render.yaml` with dynamic SSL & port injection)
  - Production Kubernetes manifests in `k8s/` (Deployments, Services, Ingress with WebSocket upgrades, HPA, ConfigMaps, Secrets)
  - Major cloud deployment blueprints (AWS ECS Fargate / RDS / S3, Google Cloud Run, Azure Container Apps, OCI Ampere)
  - GitHub Actions automated CI/CD pipeline for building, testing, and container image publishing
  - Full observability visualization (OpenTelemetry OTLP traces in Jaeger, Prometheus metrics, Grafana dashboards)

- ⬜ **Phase 10: Extended Platform Capabilities**
  - Directed Acyclic Graph (DAG) task dependency engine with cycle detection
  - Recurring scheduled task engine
  - Kanban board drag-and-drop positional reordering
  - Webhooks dispatching system for external automation

- ⬜ **Phase 11: Modern Collaborative Web Application (Frontend)**
  - Next.js (App Router) & React 19 single-page progressive web application
  - Responsive, accessible UI system using Tailwind CSS, Radix UI primitives, and Lucide icons
  - Interactive Kanban board with drag-and-drop state transitions (`@dnd-kit`)
  - Real-time notification center and live task update subscriptions via WebSocket/STOMP
  - Rich Markdown task editor with live preview and syntax highlighting
  - Multi-workspace switcher, invite code modals, and team member management dashboard
  - Embedded AI assistant panel for natural language task drafting and comment summarization
  - Client state & server synchronization via TanStack Query and Zustand

---

### Legend
- ✅ Complete
- 🔄 In Progress
- ⬜ Planned
