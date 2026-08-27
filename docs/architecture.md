# TaskMaster — System Architecture & Technical Design Specification

This document provides the definitive, production-grade technical specification of the TaskMaster platform. It documents the end-to-end architecture, C4 structural diagrams, subsystem sequences, data schemas, security perimeters, resilience mechanisms, and cloud-native deployment topology.

---

## Table of Contents
1. [Architectural Principles & System Context (C4 Level 1)](#1-architectural-principles--system-context-c4-level-1)
2. [Container Architecture & Modular Monolith Boundary (C4 Level 2)](#2-container-architecture--modular-monolith-boundary-c4-level-2)
3. [Component-Level Hexagonal Architecture (C4 Level 3)](#3-component-level-hexagonal-architecture-c4-level-3)
4. [Security Perimeter & Ingress Pipeline](#4-security-perimeter--ingress-pipeline)
5. [Subsystem Sequences & Core Workflows](#5-subsystem-sequences--core-workflows)
   - 5.1 [Asymmetric Authentication & Token Family Rotation (RS256)](#51-asymmetric-authentication--token-family-rotation-rs256)
   - 5.2 [Task Lifecycle State Machine & Concurrency Conflict Resolution](#52-task-lifecycle-state-machine--concurrency-conflict-resolution)
   - 5.3 [Threaded Discussions & Direct S3 Pre-Signed Storage](#53-threaded-discussions--direct-s3-pre-signed-storage)
   - 5.4 [Real-Time Notification & WebSocket STOMP Pipeline](#54-real-time-notification--websocket-stomp-pipeline)
   - 5.5 [Pluggable Generative AI Multi-Provider Engine](#55-pluggable-generative-ai-multi-provider-engine)
6. [Event-Driven Decoupling & Messaging Topology](#6-event-driven-decoupling--messaging-topology)
7. [Database Architecture & Entity-Relationship Schema](#7-database-architecture--entity-relationship-schema)
8. [High-Performance Data & Storage Tier Lifecycle](#8-high-performance-data--storage-tier-lifecycle)
9. [Observability, Metrics & Telemetry Pipeline](#9-observability-metrics--telemetry-pipeline)
10. [Cloud-Native Deployment & Container Topology](#10-cloud-native-deployment--container-topology)
11. [Technology Baseline Matrix](#11-technology-baseline-matrix)

---

## 1. Architectural Principles & System Context (C4 Level 1)

TaskMaster is built as a **Modular Monolith using Hexagonal Architecture (Ports & Adapters)** on **Java 25 (LTS)** and **Spring Boot 4.x (Spring Framework 7.0)**.

### Core Architectural Principles
1. **Domain-Driven Boundary Isolation**: Business logic is encapsulated in pure domain models (`user`, `task`, `team`, `collaboration`, `notification`, `ai`).
2. **Ports and Adapters (Hexagonal)**: Core business rules depend only on domain port abstractions; infrastructure adapters (PostgreSQL, Redis, MinIO/S3, LLM APIs) implement these ports.
3. **Zero-Trust Asymmetric Security**: Token verification relies on asymmetric RS256 cryptography with public RFC 7517 JWKS discovery and family-based refresh token theft mitigation.
4. **Event-Driven Decoupling**: Domain mutations emit strongly typed domain events, decoupling core transactions from side effects (notifications, audit logging, analytics).
5. **Direct Binary Storage Offloading**: Web application threads are protected from I/O exhaustion by offloading large file uploads and downloads directly to S3-compatible object stores via pre-signed URLs.

```mermaid
flowchart TB
    subgraph CLIENTS["⬡  Clients & Consumers"]
        WebUI["🌐  Web Application\nNext.js / React 19"]
        MobileAPI["📱  Mobile & API Clients\nREST / OpenAPI"]
    end

    subgraph APP["⬡  TaskMaster Platform  ·  Spring Boot 4.x / Java 25"]
        direction LR
        SecGW["🔐  Security Gateway\nOAuth2 RS256 · Rate Limiter · STOMP Auth"]
        Engine["⚙️  Domain Engine\nUser · Task · Team · Collaboration · Notification · AI"]
        SecGW --> Engine
    end

    subgraph STORAGE["⬡  Storage & Infrastructure"]
        direction LR
        PG[("🐘  PostgreSQL 17\nTransactions · tsvector · JSONB")]
        Redis[("🔴  Redis 7\nRate Limiter · Session")]
        S3[("🪣  MinIO / AWS S3\nFile Attachments")]
    end

    subgraph AI["⬡  External AI Providers"]
        LLM["🤖  Groq Cloud · Google Gemini\nOllama · AI Gateways"]
    end

    WebUI   -- "HTTPS / REST API"          --> SecGW
    WebUI   -- "WSS / WebSocket STOMP"     --> SecGW
    MobileAPI -- "HTTPS / REST API"        --> SecGW

    Engine  -- "JDBC / JPA"               --> PG
    Engine  -- "Lettuce / Redis Protocol"  --> Redis
    Engine  -- "AWS SDK v2 · Pre-Signed"   --> S3
    Engine  -- "HTTPS / RestClient"        --> LLM

    WebUI   -. "Direct Pre-Signed Upload/Download" .-> S3
```

---

## 2. Container Architecture & Modular Monolith Boundary (C4 Level 2)

The platform is structured into clear vertical domain boundaries with zero cyclical dependencies, enforced at build time via **ArchUnit**.

```mermaid
flowchart TB
    subgraph EDGE["① Edge & Transport Layer"]
        direction LR
        REST["REST Controllers\n/api/v1/*"]
        WS["WebSocket STOMP Broker\n/ws"]
    end

    subgraph BUS["② Internal Domain Event Bus\n— Spring ApplicationEventPublisher —"]
        E_ASSIGN["TaskAssignedEvent"]
        E_STATUS["TaskStatusChangedEvent"]
        E_COMMENT["TaskCommentCreatedEvent"]
        E_MEMBER["TeamMemberJoinedEvent"]
    end

    subgraph MODULES["③ Domain Modules  (Hexagonal Modular Monolith)"]
        direction LR
        USER["👤  User & Identity\nAuth · JWKS · Profile · RBAC"]
        TASK["✅  Task Management\nFSM · Criteria Filter · Optimistic Lock"]
        TEAM["👥  Team Workspace\nWorkspace Scope · Invite Code · Roles"]
        COLLAB["💬  Collaboration\nThreaded Comments · S3 Attachments"]
        NOTIF["🔔  Notifications\nSTOMP Push · Notification Center"]
        AI["🤖  AI Intelligence\nPluggable Engine · Heuristic Fallback"]
    end

    subgraph INFRA["④ Infrastructure & External Services"]
        direction LR
        PG[("PostgreSQL 17")]
        Redis[("Redis 7")]
        S3[("AWS S3 / MinIO")]
        LLM["Gemini / Groq APIs"]
    end

    %% Ingress routing
    REST --> USER
    REST --> TASK
    REST --> TEAM
    REST --> COLLAB
    REST --> NOTIF
    REST --> AI
    WS  --> NOTIF

    %% Domain event emissions
    TASK   -. "TaskAssignedEvent\nTaskStatusChangedEvent"  .-> BUS
    TEAM   -. "TeamMemberJoinedEvent"                      .-> BUS
    COLLAB -. "CommentCreatedEvent\nAttachmentUploadedEvent" .-> BUS
    BUS    -. "dispatch"                                   .-> NOTIF

    %% Infrastructure connections
    USER   --> PG
    USER   --> Redis
    TASK   --> PG
    TEAM   --> PG
    COLLAB --> PG
    COLLAB --> S3
    NOTIF  --> PG
    AI     --> LLM
```

---

## 3. Component-Level Hexagonal Architecture (C4 Level 3)

Each domain module strictly adheres to the Hexagonal (Ports & Adapters) pattern. The domain core depends on no framework or infrastructure — only on its own port abstractions.

```mermaid
flowchart LR
    subgraph IN["Inbound  (Driving) Adapters"]
        direction TB
        HTTP["REST Adapter\nController + Request DTOs\n@RestController"]
        STOMP["STOMP Adapter\nChannel Interceptor\nHandshake Auth"]
    end

    subgraph CORE["Domain Core  (Pure Java — Framework-Free)"]
        direction TB
        IPORT["Inbound Service Port\n« interface »"]
        SVC["Application Use-Case Service\nOrchestration · Validation · Events"]
        DOM["Domain Aggregate / Entity\nBusiness Rules · Invariants · State Machine"]
        OPORT["Outbound SPI Port\n« interface »"]

        IPORT --> SVC
        SVC   --> DOM
        SVC   --> OPORT
    end

    subgraph OUT["Outbound  (Driven) Adapters"]
        direction TB
        JPA["JPA Adapter\nSpring Data Repository\nEntity Mappers"]
        S3A["S3 Storage Adapter\nAWS SDK v2 Client\nPre-Signed URL Engine"]
        AIA["AI Provider Adapter\nUniversal OpenAI Client\nHeuristic Fallback"]
        EVT["Event Publisher Adapter\nSpring ApplicationEventPublisher"]
    end

    HTTP  --> IPORT
    STOMP --> IPORT

    OPORT --> JPA
    OPORT --> S3A
    OPORT --> AIA
    OPORT --> EVT
```

---

## 4. Security Perimeter & Ingress Pipeline

Every HTTP and WebSocket request traverses an ordered security and observability filter chain before reaching application handlers. Each gate returns a specific RFC 7807 `ProblemDetail` error on rejection.

```mermaid
flowchart TD
    REQ(["Incoming HTTP / WSS Request"])

    REQ      --> F1

    F1["① CorrelationIdFilter\nGenerate X-Correlation-ID\nPopulate MDC for structured logging"]
    F2["② CorsFilter\nValidate Origin against allowlist\nPreset CORS response headers"]
    F3["③ JWT Authentication Filter\nValidate RS256 signature via RSA public key\nExtract subject + roles → SecurityContext"]
    F4["④ Sliding-Window Rate Limiter\nRedis ZSET atomic window check\nkey = IP address or user-id"]
    F5["⑤ Method Security Interceptor\n@PreAuthorize role & ownership evaluation\nSpring Security AOP proxy"]
    OK(["⑥ Controller Handler Execution\nBusiness Logic Invoked"])

    E401(["401 Unauthorized\nRFC 7807 ProblemDetail"])
    E429(["429 Too Many Requests\nRetry-After header included"])
    E403(["403 Forbidden\nRFC 7807 ProblemDetail"])

    F1 --> F2
    F2 --> F3

    F3 -- "Invalid / Expired Token"    --> E401
    F3 -- "Valid JWT ✓"                --> F4

    F4 -- "Limit Exceeded"             --> E429
    F4 -- "Within Rate Limit ✓"        --> F5

    F5 -- "Insufficient Role/Ownership" --> E403
    F5 -- "Authorized ✓"               --> OK
```

---

## 5. Subsystem Sequences & Core Workflows

---

### 5.1 Asymmetric Authentication & Token Family Rotation (RS256)

```mermaid
sequenceDiagram
    autonumber

    actor     Client   as "SPA / Mobile Client"
    participant AuthCtrl as "AuthController"
    participant Redis    as "Redis  (Rate Limiter)"
    participant AuthSvc  as "AuthService"
    participant TokenSvc as "TokenService  (RS256)"
    participant DB       as "PostgreSQL  (refresh_tokens)"

    rect rgb(230, 245, 255)
        Note over Client, DB: ── Phase 1: User Login ──
        Client   ->>  AuthCtrl: POST /api/v1/auth/login  { username, password }
        AuthCtrl ->>  Redis:    Check sliding window  (5 req / min / IP)
        Redis    -->> AuthCtrl: ✓ Allowed  (count: 1 of 5)
        AuthCtrl ->>  AuthSvc:  Delegate credential verification
        AuthSvc  ->>  AuthSvc:  BCrypt.verify(password, passwordHash)
        AuthSvc  ->>  TokenSvc: Generate RS256 access token  (TTL 15 min)
        TokenSvc -->> AuthSvc:  Signed JWT
        AuthSvc  ->>  TokenSvc: Generate cryptographic refresh token  (32 random bytes)
        AuthSvc  ->>  DB:       INSERT  token_hash=SHA256(rt), family_id=UUID, is_revoked=false
        AuthSvc  -->> Client:   200 OK  { accessToken, refreshToken, expiresIn: 900 }
    end

    rect rgb(255, 245, 230)
        Note over Client, DB: ── Phase 2: Token Rotation & Replay Theft Detection ──
        Client   ->>  AuthCtrl: POST /api/v1/auth/refresh  { refreshToken }
        AuthCtrl ->>  AuthSvc:  rotateRefreshToken(rawToken)
        AuthSvc  ->>  DB:       SELECT WHERE token_hash = SHA256(refreshToken)

        alt Token is active — normal rotation
            AuthSvc  ->>  DB:      UPDATE  is_revoked = true  WHERE id = :current
            AuthSvc  ->>  TokenSvc: Generate new RS256 access token
            AuthSvc  ->>  DB:      INSERT  new token hash, same family_id
            AuthSvc  -->> Client:  200 OK  { accessToken, refreshToken (rotated) }
        else Token already revoked — REPLAY ATTACK DETECTED
            AuthSvc  ->>  DB:      UPDATE  is_revoked = true  WHERE family_id = :familyId
            AuthSvc  -->> Client:  401 Unauthorized  (token family fully invalidated)
        end
    end
```

---

### 5.2 Task Lifecycle State Machine & Concurrency Conflict Resolution

The `TaskStatus` domain enum encapsulates all valid state transitions. Invalid transitions are rejected with `400 Bad Request` at the domain boundary — no if-chains in service code.

```mermaid
stateDiagram-v2
    direction LR

    [*]         --> OPEN       : Task Created

    OPEN        --> IN_PROGRESS : Begin Work
    OPEN        --> ARCHIVED    : Cancel / Archive

    IN_PROGRESS --> REVIEW      : Submit for Review
    IN_PROGRESS --> OPEN        : Blocked / Reopen
    IN_PROGRESS --> ARCHIVED    : Abandon

    REVIEW      --> COMPLETED   : ✓ Approved
    REVIEW      --> IN_PROGRESS : ↩ Changes Requested

    COMPLETED   --> ARCHIVED    : Final Archive
    COMPLETED   --> IN_PROGRESS : Reopen Defect

    ARCHIVED    --> OPEN        : Restore

    COMPLETED   --> [*]
    ARCHIVED    --> [*]
```

#### Concurrency Conflict Prevention — `@Version` Optimistic Locking

```mermaid
sequenceDiagram
    autonumber

    actor EngrA as "Engineer A"
    actor EngrB as "Engineer B"
    participant API  as "TaskService + Controller"
    participant DB   as "PostgreSQL  (tasks · version column)"

    EngrA ->> API: GET /api/v1/tasks/101
    API  -->> EngrA: 200 OK  { status: OPEN, version: 1 }

    EngrB ->> API: GET /api/v1/tasks/101
    API  -->> EngrB: 200 OK  { status: OPEN, version: 1 }

    Note over EngrA, DB: Engineer A transitions task to IN_PROGRESS
    EngrA ->> API: PATCH /api/v1/tasks/101/status  { status: IN_PROGRESS }
    API  ->>  DB:  UPDATE tasks SET status='IN_PROGRESS', version=2\nWHERE id=101 AND version=1
    DB  -->>  API: 1 row affected ✓
    API -->> EngrA: 200 OK  { status: IN_PROGRESS, version: 2 }

    Note over EngrB, DB: Engineer B submits against stale version=1 — conflict!
    EngrB ->> API: PATCH /api/v1/tasks/101/status  { status: REVIEW }
    API  ->>  DB:  UPDATE tasks SET status='REVIEW', version=2\nWHERE id=101 AND version=1
    DB  -->>  API: 0 rows affected — OptimisticLockException
    API -->> EngrB: 409 Conflict  "Resource modified by another request. Please retry."
```

---

### 5.3 Threaded Discussions & Direct S3 Pre-Signed Storage

The application server never proxies file bytes — it delegates storage I/O to the object store directly via the AWS SDK, then issues a short-lived pre-signed URL to the client for direct download.

```mermaid
sequenceDiagram
    autonumber

    actor Client  as "Client Application"
    participant API  as "CollaborationService"
    participant S3   as "MinIO / AWS S3"
    participant DB   as "PostgreSQL"

    rect rgb(230, 255, 240)
        Note over Client, DB: ── File Upload & Metadata Persistence ──
        Client ->>  API: POST /api/v1/tasks/{id}/attachments  (multipart)
        API    ->>  API: Validate: file ≤ 10 MB · user is team member
        API    ->>  S3:  PutObject(key = tasks/{taskId}/{uuid}-{filename})
        S3    -->>  API: ETag / storage confirmation
        API    ->>  S3:  GetObjectPresignRequest(TTL = 15 min)
        S3    -->>  API: Pre-Signed Download URL
        API    ->>  DB:  INSERT task_attachments (storage_key, file_name, file_size, ...)
        API   -->>  Client: 201 Created  { id, fileName, downloadUrl, sizeBytes }
    end

    rect rgb(255, 245, 230)
        Note over Client, S3: ── Direct Client Download  (zero app-server I/O) ──
        Client ->>  S3:  GET <downloadUrl>  (authenticated via pre-signed signature)
        S3    -->>  Client: 200 OK  Binary stream  Content-Disposition: attachment
    end
```

---

### 5.4 Real-Time Notification & WebSocket STOMP Pipeline

```mermaid
sequenceDiagram
    autonumber

    actor Bob    as "Bob  (Recipient)"
    participant Broker    as "WebSocket STOMP Broker  (/ws)"
    participant Intercept as "WebSocketAuthChannelInterceptor"
    actor Alice  as "Alice  (Sender)"
    participant TaskSvc   as "TaskService"
    participant Listener  as "NotificationEventListener"
    participant NotifSvc  as "NotificationService"
    participant DB        as "PostgreSQL"

    rect rgb(230, 245, 255)
        Note over Bob, Intercept: ── Bob establishes an authenticated WebSocket session ──
        Bob      ->>  Broker:    STOMP CONNECT  { Authorization: Bearer <Bob_JWT> }
        Broker   ->>  Intercept: intercept CONNECT frame
        Intercept ->> Intercept: Parse RS256 JWT → bind Principal (Bob)
        Intercept -->> Broker:   Principal established
        Broker   -->> Bob:       STOMP CONNECTED
        Bob      ->>  Broker:    STOMP SUBSCRIBE  /user/queue/notifications
    end

    rect rgb(255, 245, 230)
        Note over Alice, DB: ── Alice assigns a task to Bob ──
        Alice    ->>  TaskSvc:  POST /api/v1/tasks  { assigneeId: Bob }
        TaskSvc  ->>  Listener: publish TaskAssignedEvent
        Listener ->>  NotifSvc: createAndSendNotification(Bob, TASK_ASSIGNED, ...)
        NotifSvc ->>  DB:       INSERT notifications  (recipient=Bob, is_read=false)
        NotifSvc ->>  Broker:   SimpMessagingTemplate.convertAndSendToUser\n(Bob, "/queue/notifications", payload)
        Broker   -->> Bob:      STOMP MESSAGE  🔔  pushed in real-time
    end
```

---

### 5.5 Pluggable Generative AI Multi-Provider Engine

The `PluggableAiProvider` sends a standard `POST /chat/completions` request using the OpenAI-compatible schema. Any compliant endpoint — cloud provider, local model, or AI gateway — is reachable by changing a single environment variable.

```mermaid
flowchart TD
    REQ(["Client Request\nPOST /api/v1/ai/*"])

    REQ      --> CTRL["AiController"]
    CTRL     --> SVC["AiAssistantService\nPrompt construction · Context gathering · Response parsing"]
    SVC      --> PORT["AiProvider Port\n« interface »"]
    PORT     --> ENG["Universal OpenAI-Compatible Client\nPOST /chat/completions · Spring RestClient"]

    ENG      --> ROUTE{{"AI_BASE_URL\nRuntime Environment Variable"}}

    ROUTE    -- "Cloud provider" --> CLOUD["☁️  Groq Cloud / Google Gemini\nllama-3.3-70b · gemini-2.5-flash"]
    ROUTE    -- "Local engine"   --> LOCAL["🖥️  Ollama / vLLM\nllama3.2 · deepseek-r1"]
    ROUTE    -- "AI Gateway"     --> GW["🔀  LiteLLM / Portkey / Kong\nCaching · Load Balancing · Failover"]

    CLOUD    -- "HTTP 200 ✓"     --> PARSE
    LOCAL    -- "HTTP 200 ✓"     --> PARSE
    GW       -- "HTTP 200 ✓"     --> PARSE

    CLOUD    -. "429 / 503 / Timeout" .-> FALLBACK
    LOCAL    -. "Connection Refused"  .-> FALLBACK
    GW       -. "Upstream Degraded"   .-> FALLBACK

    FALLBACK["🛡️  Heuristic Fallback Engine\nContext-aware keyword analysis\nDeterministic synthetic generation"]
    FALLBACK --> PARSE

    PARSE["Response Parser\nJSON extraction · Markdown structuring"]
    PARSE    --> RESP(["Structured API Response\nMarkdown · JSON · Priority · Labels"])
```

---

## 6. Event-Driven Decoupling & Messaging Topology

Domain events isolate transaction boundaries and guarantee loose coupling between aggregates. The event bus decouples producers from consumers — neither side references the other.

```mermaid
flowchart LR
    subgraph PROD["Event Producers"]
        direction TB
        TaskAgg["Task Aggregate\nCreate · StatusChange · Assign · Delete"]
        TeamAgg["Team Aggregate\nMemberJoined · RoleChanged"]
        CollAgg["Collaboration Aggregate\nCommentCreated · FileUploaded"]
    end

    subgraph EVENTS["Domain Event Types"]
        direction TB
        EV1["TaskAssignedEvent"]
        EV2["TaskStatusChangedEvent"]
        EV3["TaskCreatedEvent"]
        EV4["TeamMemberJoinedEvent"]
        EV5["TaskCommentCreatedEvent"]
    end

    subgraph CONS["Event Consumers"]
        direction TB
        NotifL["NotificationEventListener\nDB persist + STOMP real-time push"]
        AuditL["AuditLogEventListener\nCompliance & history recording\n(Phase 6)"]
        SearchL["SearchIndexEventListener\nElasticsearch sync\n(Phase 6)"]
    end

    TaskAgg --> EV1
    TaskAgg --> EV2
    TaskAgg --> EV3
    TeamAgg --> EV4
    CollAgg --> EV5

    EV1 --> NotifL
    EV2 --> NotifL
    EV4 --> NotifL
    EV5 --> NotifL

    EV1 --> AuditL
    EV2 --> AuditL
    EV3 --> AuditL

    EV2 --> SearchL
    EV3 --> SearchL
```

---

## 7. Database Architecture & Entity-Relationship Schema

```mermaid
erDiagram
    USERS ||--o{ REFRESH_TOKENS   : "owns"
    USERS ||--o{ TEAM_MEMBERS     : "joins"
    USERS ||--o{ TASKS            : "creates"
    USERS ||--o{ TASKS            : "is assigned"
    USERS ||--o{ TASK_COMMENTS    : "authors"
    USERS ||--o{ TASK_ATTACHMENTS : "uploads"
    USERS ||--o{ NOTIFICATIONS    : "receives"

    TEAMS ||--o{ TEAM_MEMBERS     : "includes"
    TEAMS ||--o{ TASKS            : "scopes"

    TASKS ||--o{ TASK_COMMENTS    : "has"
    TASKS ||--o{ TASK_ATTACHMENTS : "has"
    TASKS ||--o{ TASK_LABELS      : "tagged with"

    TASK_COMMENTS ||--o{ TASK_COMMENTS : "replies to"

    USERS {
        uuid        id           PK
        varchar     email        UK
        varchar     username     UK
        varchar     password_hash
        varchar     display_name
        varchar     avatar_url
        varchar     role         "USER | ADMIN"
        boolean     is_active
        timestamptz created_at
        timestamptz updated_at
    }

    REFRESH_TOKENS {
        uuid        id           PK
        uuid        user_id      FK
        uuid        family_id    "rotation family"
        varchar     token_hash   UK  "SHA-256 hash"
        boolean     is_revoked
        timestamptz expires_at
        timestamptz created_at
    }

    TEAMS {
        uuid        id           PK
        varchar     name
        text        description
        uuid        owner_id     FK
        varchar     invite_code  UK
        timestamptz created_at
        timestamptz updated_at
    }

    TEAM_MEMBERS {
        uuid        id           PK
        uuid        team_id      FK
        uuid        user_id      FK
        varchar     role         "OWNER | ADMIN | MEMBER"
        timestamptz joined_at
    }

    TASKS {
        uuid        id           PK
        varchar     title
        text        description
        varchar     status       "OPEN | IN_PROGRESS | REVIEW | COMPLETED | ARCHIVED"
        varchar     priority     "LOW | MEDIUM | HIGH | URGENT"
        uuid        creator_id   FK
        uuid        assignee_id  FK
        uuid        team_id      FK
        tsvector    search_vector "GIN indexed — weighted FTS"
        bigint      version      "optimistic lock"
        timestamptz due_date
        timestamptz deleted_at   "soft delete"
        timestamptz created_at
        timestamptz updated_at
    }

    TASK_LABELS {
        uuid        task_id      FK
        varchar     label
    }

    TASK_COMMENTS {
        uuid        id           PK
        uuid        task_id      FK
        uuid        author_id    FK
        uuid        parent_id    FK  "nullable — threading"
        text        content
        timestamptz deleted_at
        timestamptz created_at
        timestamptz updated_at
    }

    TASK_ATTACHMENTS {
        uuid        id           PK
        uuid        task_id      FK
        uuid        uploader_id  FK
        varchar     file_name
        varchar     content_type
        bigint      file_size
        varchar     storage_key  "S3/MinIO object key"
        timestamptz created_at
    }

    NOTIFICATIONS {
        uuid        id           PK
        uuid        recipient_id FK
        varchar     type         "TASK_ASSIGNED | COMMENT_ADDED | TEAM_INVITE | TASK_UPDATED"
        varchar     title
        text        message
        jsonb       metadata     "taskId · commentId · teamId"
        boolean     is_read
        timestamptz read_at
        timestamptz created_at
    }
```

### High-Performance Indexing Strategy

| Index | SQL Definition | Purpose |
|:---|:---|:---|
| **Full-Text Search (GIN)** | `CREATE INDEX idx_tasks_fts ON tasks USING GIN (search_vector)` | Sub-millisecond full-text search on title + description |
| **Active Task Query** | `CREATE INDEX idx_tasks_team_active ON tasks (team_id, status) WHERE deleted_at IS NULL` | Team dashboard queries — partial index avoids deleted rows |
| **Unread Notifications** | `CREATE INDEX idx_notif_unread ON notifications (recipient_id) WHERE is_read = FALSE` | Unread badge count — partial index, extremely fast |
| **Comment Thread** | `CREATE INDEX idx_comments_thread ON task_comments (task_id, parent_id) WHERE deleted_at IS NULL` | Threaded comment tree reconstruction |
| **Token Lookup** | `CREATE INDEX idx_refresh_token_hash ON refresh_tokens (token_hash)` | O(log n) token verification on every `/auth/refresh` |

---

## 8. High-Performance Data & Storage Tier Lifecycle

```mermaid
flowchart TB
    subgraph HOT["🔴  Hot Tier — Redis 7  (In-Memory, Sub-millisecond)"]
        direction LR
        RL["Sliding-Window Rate Limiter\nZSET per IP / user · TTL 60 s"]
        WS["WebSocket Session Registry\nConnected user → session mapping"]
    end

    subgraph WARM["🐘  Warm Tier — PostgreSQL 17  (Transactional, Persistent)"]
        direction LR
        REL["Relational Domain Tables\nUsers · Teams · Tasks · Comments · Attachments"]
        FTS["Full-Text Search Engine\ntsvector GIN index · weighted ranking"]
        MIG["Schema Migration Log\nFlyway — immutable, versioned"]
    end

    subgraph COLD["🪣  Object Storage Tier — MinIO / AWS S3  (Durable, Scalable)"]
        direction LR
        BLOB["Task File Attachments\nImmutable binary blobs · AES-256 at rest"]
        PS["Pre-Signed Download URLs\nTime-limited (15 min) · Direct client stream"]
    end

    HOT  -. "TTL eviction"  .-> WARM
    WARM -- "metadata query" --> COLD
```

---

## 9. Observability, Metrics & Telemetry Pipeline

TaskMaster implements enterprise-grade observability following the **OpenTelemetry** and **Prometheus** standards. Every request carries a correlation ID through the full call stack, enabling end-to-end distributed tracing.

```mermaid
flowchart LR
    subgraph APP["TaskMaster Runtime  (Spring Boot 4.x)"]
        direction TB
        CID["CorrelationIdFilter\nX-Correlation-ID → MDC → all log lines"]
        PROM["Micrometer Prometheus\n/actuator/prometheus — metrics endpoint"]
        HEALTH["Health Probes\n/actuator/health/liveness\n/actuator/health/readiness"]
        ERR["RFC 7807 Error Envelope\nProblemDetail on every exception"]
    end

    subgraph COLLECT["Observability Ingestion"]
        direction TB
        PromSrv["Prometheus Server\nscrapes every 15 s"]
        Loki["Grafana Loki / FluentBit\nstructured JSON log aggregation"]
    end

    subgraph DASH["Dashboards & Alerting"]
        direction TB
        Grafana["Grafana Dashboards\nHTTP latency · DB pool · JVM · Virtual thread saturation"]
        Alerts["AlertManager / PagerDuty\nHigh error rate · DB connection exhaustion"]
    end

    PROM   --> PromSrv
    CID    --> Loki
    PromSrv --> Grafana
    PromSrv --> Alerts
```

---

## 10. Cloud-Native Deployment & Container Topology

```mermaid
flowchart TB
    subgraph INGRESS["☁️  Cloud Ingress"]
        LB["Kubernetes NGINX / Cloud Load Balancer\nTLS Termination · HTTP/2 · WSS Upgrade Routing"]
    end

    subgraph K8S["Kubernetes Cluster  (Production Namespace)"]
        subgraph HPA["TaskMaster Pods  — HPA Auto-scaling"]
            P1["Pod 1\nJava 25 Virtual Threads\nGraceful Shutdown: 30 s"]
            P2["Pod 2\nJava 25 Virtual Threads\nGraceful Shutdown: 30 s"]
            PN["Pod N\nJava 25 Virtual Threads\nGraceful Shutdown: 30 s"]
        end
    end

    subgraph MANAGED["Managed Cloud Infrastructure"]
        direction LR
        PG[("PostgreSQL 17\nPrimary + Read Replica")]
        RD[("Redis 7\nHA Sentinel Cluster")]
        OBJ[("AWS S3 / Cloudflare R2\nMulti-Region Object Storage")]
    end

    LB --> P1
    LB --> P2
    LB --> PN

    P1 --> PG
    P2 --> PG
    PN --> PG

    P1 --> RD
    P2 --> RD
    PN --> RD

    P1 --> OBJ
    P2 --> OBJ
    PN --> OBJ
```

---

## 11. Technology Baseline Matrix

| Tier | Component | Selection | Architectural Rationale |
|:---|:---|:---|:---|
| **Runtime** | Language | **Java 25 (LTS)** | Virtual Threads (Project Loom), Records, Sealed Types, Pattern Matching. |
| **Framework** | Application Engine | **Spring Boot 4.x / Spring 7.0** | Jakarta EE 11 baseline, centralized BOM, RFC 7807 `ProblemDetail`. |
| **Security** | Auth & Signing | **Spring Security + Nimbus JOSE** | Asymmetric RS256 JWT, Public RFC 7517 JWKS endpoint, family-based replay mitigation. |
| **Database** | Relational Datastore | **PostgreSQL 17** | ACID transactions, Flyway migrations, `tsvector` GIN full-text search, JSONB. |
| **In-Memory** | Cache & Rate Limiter | **Redis 7 (Lettuce Client)** | Atomic ZSET sliding-window rate limiting, WebSocket session registry. |
| **Storage** | Object Storage | **MinIO / AWS S3 (AWS SDK v2)** | Direct pre-signed URL upload/download — zero app-server I/O bottleneck. |
| **Real-time** | Push Broker | **WebSocket + STOMP (SockJS)** | JWT-authenticated sessions, `convertAndSendToUser` point-to-point routing. |
| **AI Assistant** | Generative AI | **Universal OpenAI-Compatible Client** | Single `/chat/completions` adapter supporting Groq, Gemini, Ollama, AI Gateways. |
| **Observability** | Telemetry & Tracing | **OpenTelemetry (OTel) + OTLP** | CNCF standard vendor-neutral distributed tracing, W3C TraceContext, and Micrometer bridge. |
| **Mapping** | Object Mapping | **MapStruct 1.6** | Compile-time type-safe DTO ↔ Entity mapping — zero reflection overhead. |
| **API Docs** | Specification | **SpringDoc OpenAPI 3.1** | Auto-generated, always-in-sync Swagger UI at `/swagger-ui.html` + `docs/api/openapi.yaml`. |
| **Quality** | Testing & Verification | **ArchUnit + Testcontainers + Checkstyle** | Compile-time hexagonal boundary enforcement + containerised integration test slices. |
