# ADR 0001: Adopt Modular Monolith Architecture

## Status
Accepted

## Context
When designing the backend for TaskMaster, we considered whether to adopt a microservices architecture or a monolithic architecture. 

A microservices architecture introduces significant operational overhead early on:
- Distributed transaction coordination
- Network latency and serialization costs
- Complex local development and deployment pipelines
- Service discovery, distributed tracing, and configuration management overhead

## Decision
We will build TaskMaster as a **Modular Monolith** using **Spring Boot 4.x (Spring Framework 7.0)** and **Java 25 (LTS)**. 

Key principles:
1. Modules correspond to bounded business contexts: `user`, `task`, `team`, `collaboration`, `notification`, and `ai`.
2. Cross-module communication is structured through well-defined service interfaces and domain events.
3. Code dependencies between modules are strictly enforced using ArchUnit tests to prevent tight coupling.
4. **Technology Baseline & BOM Management**: Built on Java 25 (Virtual Threads, Records) and Spring Boot 4.x using centralized Bill of Materials (`spring-boot-dependencies`) to guarantee zero starter version drift across all modular dependencies.

## Consequences
### Positive
- Rapid development velocity with zero cross-network service calls during core workflows.
- Single deployment artifact with simplified CI/CD and operational monitoring.
- Clear module boundaries allow decomposing specific modules into independent microservices in the future if scaling requirements demand it.
- Synchronized starter dependencies managed automatically via the root Spring Boot BOM.

### Negative
- All modules share the same runtime process and memory space in the initial phases.
- Scaling requires scaling the entire application instance unless extracted later.
