# ADR 0009: Multi-Tenant Task and Workspace Authorization Enforcement

- **Status**: Accepted
- **Approval Date**: 2026-09-08
- **Author**: TaskMaster Engineering (`shashakchandel@gmail.com`)

---

## Context

TaskMaster supports collaborative team workspaces as well as private personal tasks. Tasks without an associated team (`teamId == null`) represent personal workflows, while tasks with a `teamId` belong to shared workspaces governed by team roles (`OWNER`, `ADMIN`, `MEMBER`).

A security evaluation identified two critical authorization vulnerabilities:
1. **Unscoped Task Queries**: The task filtering endpoint (`GET /api/v1/tasks`) did not bind the authenticated user identity or validate workspace membership, allowing users to discover tasks from other teams or view other users' private personal tasks.
2. **Personal Task IDOR Vulnerability**: Task access checks used a short-circuit guard `if (task.getTeamId() != null && !existsByTeamIdAndUserId(...))`. When `teamId == null`, this check was skipped, permitting any authenticated user knowing a task UUID to read, modify, assign, or delete personal tasks.
3. **Unauthenticated Service Overload**: An overloaded service method `getTaskById(UUID taskId)` bypassed security context validation.

## Decision

We mandate comprehensive, zero-trust authorization enforcement across personal tasks, team workspaces, and task search queries:

1. **Ingress Identity Binding**:
   Extract `currentUserId` directly from `@AuthenticationPrincipal Jwt` in `TaskController.searchTasks(...)` and all mutating endpoints.
2. **Explicit Team Search Verification**:
   If a search query specifies `teamId != null`, `TaskService` must verify that `currentUserId` is an active member of that team; otherwise, return RFC 7807 `403 Forbidden`.
3. **Dynamic Database Scoping**:
   If `teamId` is omitted (`null`), query all team IDs the caller belongs to (`allowedTeamIds`) and pass them to `TaskSpecification`. Dynamically construct a database predicate:
   ```sql
   WHERE (team_id IN (:allowedTeamIds))
      OR (team_id IS NULL AND (created_by = :currentUserId OR assignee_id = :currentUserId))
   ```
4. **Explicit Personal Task Authorization Boundary**:
   For tasks where `teamId == null`:
   - Read, update, and status transitions require `currentUserId` to be either the task creator or assignee.
   - Assignment and deletion require `currentUserId` to be the task creator.
   - Strangers receive RFC 7807 `403 Forbidden` (`You do not have permission to access this task`).
5. **Subsystem Authorization Parity**:
   Enforce identical personal task and team workspace checks across comments (`TaskCommentService`), file attachments (`TaskAttachmentService`), and AI assistant operations (`AiAssistantService`).
6. **Elimination of Unauthenticated Overloads**:
   Permanently remove `getTaskById(UUID taskId)` without user context from `TaskService`.
7. **Negative Authorization Test Suites**:
   Maintain comprehensive negative unit, controller, and integration tests verifying HTTP `403 Forbidden` for stranger personal task access and unjoined team queries.

## Consequences

### Positive
- **Eliminates IDOR & Data Leakage**: Personal tasks and team backlogs are strictly partitioned by tenant and ownership boundaries.
- **Database-Level Query Scoping**: Performance is preserved by filtering unauthorized rows directly in SQL rather than post-filtering in memory.
- **Defense in Depth**: Access control is enforced in domain services regardless of the ingress transport (REST, internal events, or future RPC).

### Trade-offs
- An additional query to `team_members` is executed during global task searches when `teamId` is not specified to resolve `allowedTeamIds`. This query is indexed on `idx_team_members_user_id` and has negligible latency.
