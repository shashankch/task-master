# ADR 0003: Hexagonal (Ports and Adapters) Architecture

## Status
Accepted

## Context
In traditional layered architectures, business domain models frequently become coupled to ORM entities, web frameworks, or third-party SDKs. This makes unit testing tedious and complicates switching infrastructure providers (e.g., changing storage providers from MinIO to AWS S3).

## Decision
Each functional module (`user`, `task`, `team`, `collaboration`, `notification`, `ai`) follows the **Hexagonal (Ports and Adapters)** architecture:

- **Domain Layer (`domain/`)**: Pure business logic, entity aggregates, value objects, and domain events. Contains port interfaces and has zero external framework dependencies.
- **Application Layer (`application/`)**: Use-case orchestration, application services, DTOs, and mapping logic.
- **Adapter Layer (`adapter/`)**:
  - *Inbound (`adapter/in/`)*: REST controllers, WebSocket handlers, event listeners.
  - *Outbound (`adapter/out/`)*: JPA repository implementations, external clients (S3/MinIO, Spring AI, RabbitMQ).

## Consequences
### Positive
- Domain business logic is decoupled from frameworks and easily testable without loading Spring contexts.
- Infrastructure adapters can be swapped or modified with zero changes to business use cases.
- ArchUnit tests enforce architectural boundaries programmatically.

### Negative
- Slightly higher initial boilerplate (DTOs, interfaces, and mappers).
