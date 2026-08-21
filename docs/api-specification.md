# TaskMaster API Specification

Comprehensive REST API reference for the TaskMaster collaborative task tracking platform.

- **Base URL**: `http://localhost:8080/api/v1` (Development)
- **Content-Type**: `application/json`
- **Interactive Documentation**: [Swagger UI](http://localhost:8080/swagger-ui.html) | [OpenAPI Spec](http://localhost:8080/v3/api-docs)

---

## 🔐 Authentication & Security

TaskMaster uses stateless **asymmetric RS256 JWT access tokens** (15-minute expiry) paired with **family-based refresh tokens** (7-day expiry) stored as SHA-256 hashes with automated replay/theft detection.

### Headers

| Header | Description | Required On |
|:---|:---|:---|
| `Authorization` | `Bearer <access_token>` | All protected endpoints |
| `X-Correlation-ID` | Client request trace ID (propagated to responses and logs) | Optional (auto-generated if omitted) |

### Public JWKS Endpoint

Public RSA keys for validating access tokens independently are published at:
`GET /api/v1/auth/.well-known/jwks.json`

---

## 🚦 Rate Limiting

Sensitive authentication endpoints enforce IP-based sliding window rate limits:
- **Default Limit**: 5 requests per minute per IP.
- **Header Feedback**: 
  - `Retry-After: <seconds>` on `429 Too Many Requests`.

---

## 📄 Pagination & Sorting

All collection endpoints support standardized pagination and multi-field sorting:

### Query Parameters

| Parameter | Type | Default | Description |
|:---|:---|:---|:---|
| `page` | `integer` | `0` | Zero-based page index |
| `size` | `integer` | `20` | Page size limit (max 100) |
| `sort` | `string` | `createdAt,desc` | Sort field and direction (`field,asc` or `field,desc`) |

### Standard `PageResponse<T>` Envelope

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3,
    "first": true,
    "last": false,
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": "2026-08-21T10:00:00Z"
}
```

---

## ⚠️ Error Handling (RFC 7807)

All non-2xx responses adhere to the **RFC 7807 `ProblemDetail`** standard:

```json
{
  "type": "https://api.taskmaster.io/errors/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Input validation failed for 2 field(s)",
  "instance": "/api/v1/tasks",
  "timestamp": "2026-08-21T10:00:00Z",
  "errors": [
    {
      "field": "title",
      "rejectedValue": "",
      "message": "Title is required"
    }
  ]
}
```

---

## 📚 Endpoint Catalog

### 1. Authentication (`/api/v1/auth`)

#### 1.1 Register User
- **Method**: `POST /api/v1/auth/register`
- **Auth**: None
- **Request Body**:
```json
{
  "email": "alex.dev@example.com",
  "username": "alexdev",
  "password": "Password@123",
  "displayName": "Alex Developer"
}
```
- **Response**: `201 Created`
```json
{
  "success": true,
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "alex.dev@example.com",
    "username": "alexdev",
    "displayName": "Alex Developer",
    "avatarUrl": null,
    "role": "USER",
    "active": true,
    "createdAt": "2026-08-21T10:00:00Z",
    "updatedAt": "2026-08-21T10:00:00Z"
  },
  "timestamp": "2026-08-21T10:00:00Z"
}
```

#### 1.2 Login
- **Method**: `POST /api/v1/auth/login`
- **Auth**: None (Rate Limited)
- **Request Body**:
```json
{
  "usernameOrEmail": "alexdev",
  "password": "Password@123"
}
```
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "4a7f9b8c2d1e0f3a6b5c4d3e2f1a0b9c...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "email": "alex.dev@example.com",
      "username": "alexdev",
      "displayName": "Alex Developer",
      "role": "USER",
      "active": true
    }
  },
  "timestamp": "2026-08-21T10:00:00Z"
}
```

#### 1.3 Refresh Access Token
- **Method**: `POST /api/v1/auth/refresh`
- **Auth**: None (Rate Limited)
- **Request Body**:
```json
{
  "refreshToken": "4a7f9b8c2d1e0f3a6b5c4d3e2f1a0b9c..."
}
```
- **Response**: `200 OK` (Rotates and returns fresh access token and refresh token pair)

#### 1.4 Logout
- **Method**: `POST /api/v1/auth/logout`
- **Auth**: None
- **Request Body**:
```json
{
  "refreshToken": "4a7f9b8c2d1e0f3a6b5c4d3e2f1a0b9c..."
}
```
- **Response**: `200 OK`

#### 1.5 Public JWKS Key Set
- **Method**: `GET /api/v1/auth/.well-known/jwks.json`
- **Auth**: None
- **Response**: `200 OK` (`application/json`)

---

### 2. User Profile (`/api/v1/users`)

#### 2.1 Get Current User Profile
- **Method**: `GET /api/v1/users/me`
- **Auth**: `Bearer <token>`
- **Response**: `200 OK` (`UserResponse`)

#### 2.2 Update Current User Profile
- **Method**: `PUT /api/v1/users/me`
- **Auth**: `Bearer <token>`
- **Request Body**:
```json
{
  "displayName": "Alex Senior Dev",
  "avatarUrl": "https://cdn.taskmaster.io/avatars/alex.png"
}
```
- **Response**: `200 OK` (`UserResponse`)

---

### 3. Task Management (`/api/v1/tasks`)

#### 3.1 Create Task
- **Method**: `POST /api/v1/tasks`
- **Auth**: `Bearer <token>`
- **Request Body**:
```json
{
  "title": "Implement Redis Cache Invalidation Pipeline",
  "description": "Integrate Redis event-driven cache invalidation hooks for task state changes.",
  "priority": "HIGH",
  "dueDate": "2026-08-28T18:00:00Z",
  "assigneeId": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
  "teamId": null,
  "labels": ["backend", "redis", "performance"]
}
```
- **Response**: `201 Created`
```json
{
  "success": true,
  "data": {
    "id": "c3d4e5f6-a7b8-9012-cdef-345678901234",
    "title": "Implement Redis Cache Invalidation Pipeline",
    "description": "Integrate Redis event-driven cache invalidation hooks for task state changes.",
    "status": "OPEN",
    "priority": "HIGH",
    "dueDate": "2026-08-28T18:00:00Z",
    "creator": {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "username": "alexdev",
      "displayName": "Alex Developer"
    },
    "assignee": {
      "id": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
      "username": "sarah",
      "displayName": "Sarah Engineer"
    },
    "teamId": null,
    "labels": ["backend", "redis", "performance"],
    "version": 0,
    "createdAt": "2026-08-21T10:00:00Z",
    "updatedAt": "2026-08-21T10:00:00Z"
  },
  "timestamp": "2026-08-21T10:00:00Z"
}
```

#### 3.2 Get Task by ID
- **Method**: `GET /api/v1/tasks/{id}`
- **Auth**: `Bearer <token>`
- **Response**: `200 OK` (`TaskResponse`)

#### 3.3 Search & Filter Tasks
- **Method**: `GET /api/v1/tasks`
- **Auth**: `Bearer <token>`
- **Query Filters**:
  - `status` (e.g. `OPEN`, `IN_PROGRESS`, `REVIEW`, `COMPLETED`, `ARCHIVED`)
  - `priority` (e.g. `LOW`, `MEDIUM`, `HIGH`, `URGENT`)
  - `assigneeId` (UUID)
  - `creatorId` (UUID)
  - `teamId` (UUID)
  - `search` (Full-text keyword search across title & description)
  - `dueDateFrom` (ISO-8601 Instant)
  - `dueDateTo` (ISO-8601 Instant)
  - `label` (String tag filter)
  - `page`, `size`, `sort`
- **Response**: `200 OK` (`PageResponse<TaskResponse>`)

#### 3.4 Update Task Details
- **Method**: `PUT /api/v1/tasks/{id}`
- **Auth**: `Bearer <token>`
- **Request Body**:
```json
{
  "title": "Updated Task Title",
  "description": "Updated detailed specification",
  "priority": "URGENT",
  "dueDate": "2026-08-30T12:00:00Z",
  "labels": ["backend", "priority-fix"]
}
```
- **Response**: `200 OK` (`TaskResponse`)

#### 3.5 Lifecycle Status Transition
- **Method**: `PATCH /api/v1/tasks/{id}/status`
- **Auth**: `Bearer <token>`
- **Request Body**:
```json
{
  "status": "IN_PROGRESS"
}
```
- **State Machine Transitions**:
  - `OPEN` ➔ `IN_PROGRESS`, `ARCHIVED`
  - `IN_PROGRESS` ➔ `REVIEW`, `OPEN`, `ARCHIVED`
  - `REVIEW` ➔ `COMPLETED`, `IN_PROGRESS`, `ARCHIVED`
  - `COMPLETED` ➔ `ARCHIVED`, `OPEN`
  - `ARCHIVED` ➔ `OPEN`
- **Response**: `200 OK` (`TaskResponse`)

#### 3.6 Assign / Unassign Task
- **Method**: `PATCH /api/v1/tasks/{id}/assign`
- **Auth**: `Bearer <token>`
- **Request Body**:
```json
{
  "assigneeId": "b2c3d4e5-f6a7-8901-bcde-f23456789012"
}
```
*(Pass `assigneeId: null` to unassign)*
- **Response**: `200 OK` (`TaskResponse`)

#### 3.7 Soft Delete Task
- **Method**: `DELETE /api/v1/tasks/{id}`
- **Auth**: `Bearer <token>`
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Task successfully deleted"
  },
  "timestamp": "2026-08-21T10:00:00Z"
}
```
