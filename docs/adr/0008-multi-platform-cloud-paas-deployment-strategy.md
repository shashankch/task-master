# ADR 0008: Multi-Platform Cloud, PaaS & Container Deployment Strategy

## Status
Accepted

## Context
TaskMaster is designed as a cloud-native collaborative task platform that must run seamlessly across diverse hosting environments—from a developer's local laptop to enterprise Kubernetes clusters and modern Platform-as-a-Service (PaaS) providers—without requiring code modifications or proprietary vendor lock-in.

Modern engineering teams require flexibility across four distinct deployment tiers:
1. **Self-Hosting & Local Development**: Instant, zero-cloud-cost stack orchestration on bare metal or local workstations.
2. **Modern PaaS (Railway, Render, Fly.io)**: 1-click deployment with automated SSL, dynamic port binding, and managed database provisioning.
3. **Containerization & Orchestration (Docker & Kubernetes)**: Production-grade autoscaling, rolling zero-downtime deployments, and pod health monitoring.
4. **Major Cloud Hyperscalers (AWS, GCP, Azure, OCI)**: Scalable, enterprise infrastructure leveraging managed relational databases, object storage, and distributed telemetry.

## Decision
We adopt a **Unified Multi-Platform Deployment Strategy** in Phase 9 that guarantees portability across all four deployment tiers while maintaining 100% backward compatibility with our existing local development workflow:

| Deployment Tier | Target Platform | Orchestration & Artifacts | Configuration & Ingress Mechanism |
|:---|:---|:---|:---|
| **1. Self-Hosting & Local** | Local workstation / Bare-metal VM | `docker-compose.yml` (PostgreSQL 17, Redis 7, RabbitMQ 4, MinIO, Jaeger, Elasticsearch) | `dev` profile (`application-dev.yml`), localhost port bindings |
| **2. Modern PaaS** | Railway, Render, Fly.io | `railway.json`, `render.yaml` Infrastructure-as-Code blueprints | `prod` profile (`application-prod.yml`), dynamic `${PORT:8080}` injection, managed PostgreSQL/Redis SSL connections |
| **3. Container Orchestration** | Kubernetes (EKS, GKE, AKS, Vanilla K8s) | Minimal Distroless Java 25 Dockerfile, production `k8s/` manifests (Deployments, Services, Ingress, HPA, ConfigMaps, Secrets) | Native `/actuator/health/liveness` and `/actuator/health/readiness` probes, WebSocket upgrade annotations |
| **4. Cloud Hyperscalers** | AWS ECS Fargate, Google Cloud Run, Azure Container Apps, OCI Ampere | Multi-arch container image + Terraform / Cloud blueprints | Cloud-managed Aurora/Cloud SQL, ElastiCache, S3/R2 storage, and OpenTelemetry OTLP exporters |

### Architectural Enablers
- **12-Factor App Methodology**: All infrastructure connection strings, credentials, and tuning parameters in `application-prod.yml` bind to standard environment variables (`SPRING_DATASOURCE_URL`, `SPRING_REDIS_HOST`, `AWS_S3_ENDPOINT`, `PORT`).
- **Dynamic Port Ingress**: The application server binds to `server.port=${PORT:8080}`, automatically adapting to PaaS ingress proxies (Railway, Render, Cloud Run).
- **Orchestrator Probes**: Spring Boot Actuator exposes dedicated liveness and readiness probe groups (`/actuator/health/liveness`, `/actuator/health/readiness`) for automated container restarts and zero-downtime routing.

## Consequences
### Positive
- **Total Operational Portability**: Deployable anywhere—from a single laptop using Docker Compose to global Kubernetes clusters or zero-management PaaS.
- **Zero Code Refactoring**: Switching between local Docker, Railway, Kubernetes, or AWS ECS requires only environment variable configuration.
- **Production Resilience**: Multi-stage distroless container images minimize attack surface, reduce image size, and leverage Class Data Sharing (CDS) for rapid cold-start times.

### Negative
- Requires maintaining deployment blueprints (`railway.json`, `render.yaml`, and `k8s/` manifests) alongside application code.
