# ADR 0004: Event-Driven Messaging with Spring Cloud Stream & RabbitMQ

## Status
Accepted

## Context
Asynchronous domain events (e.g. task assignments, mentions, status updates) require reliable decoupling between event publication and consumption (such as generating notification records and dispatching real-time WebSocket pushes).

We considered:
1. Direct RabbitMQ client (`RabbitTemplate` / `@RabbitListener`)
2. Direct Kafka client (`KafkaTemplate` / `@KafkaListener`)
3. Spring Cloud Stream binder abstraction

## Decision
We choose **Spring Cloud Stream** with the **RabbitMQ Binder** for initial phases, following the functional programming model (`java.util.function.Consumer`, `Function`, `Supplier`).

Why this approach:
- Clean functional abstraction in code without broker-specific annotations.
- RabbitMQ offers lightweight local deployment and reliable message queuing for task notifications.
- When high-volume event streaming or event replay becomes necessary (e.g., in Phase 6+ for distributed analytics), the binder can be switched to **Apache Kafka** via configuration and dependency updates with zero changes to business use cases.

## Consequences
### Positive
- Unified messaging code agnostic of the underlying message broker.
- Resilient dead-letter queuing (DLQ) and retry mechanisms built into Spring Cloud Stream.
- Seamless transition path from RabbitMQ to Kafka when scaling demands.

### Negative
- High-level binder abstraction hides some fine-grained broker-specific features.
