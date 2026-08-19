# ADR 0002: Use PostgreSQL 17 as Primary Datastore

## Status
Accepted

## Context
TaskMaster requires robust data storage supporting relational entities (users, teams, memberships, tasks, comments, attachments) with strict transactional consistency guarantees. We evaluated relational (PostgreSQL, MySQL) and document-oriented (MongoDB) databases.

Task management workflows inherently demand ACID guarantees:
- Assigning tasks must consistently enforce membership rules.
- Status changes and audit logs must remain strictly synchronized.
- Semi-structured attributes like labels and metadata require flexible storage.
- Search queries need full-text search capabilities.

## Decision
We select **PostgreSQL 17** as the primary datastore managed via **Flyway** schema migrations.

Key capabilities leveraged:
1. **ACID Transactions**: Strong relational consistency across tasks, teams, and comments.
2. **JSONB Data Type**: High-performance indexed storage for flexible tags, labels, and event metadata.
3. **Full-Text Search (`tsvector` & GIN Indexes)**: Native linguistic indexing on task titles and descriptions.
4. **pgvector Ready**: Native vector embeddings support for future semantic search enhancements.

## Consequences
### Positive
- Enterprise-grade reliability and widespread cloud support (AWS RDS, OCI Database, GCP Cloud SQL).
- Hybrid capability combining relational integrity with document-style flexibility via JSONB.
- Predictable performance with indexed query execution.

### Negative
- Schema evolution requires disciplined, versioned migration scripts (mitigated by Flyway).
