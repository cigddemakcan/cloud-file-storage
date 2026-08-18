# ☁️ Cloud-Based File Storage System

A secure cloud file storage backend built with **Spring Boot**, **PostgreSQL**, and **MinIO**.

The project provides a Google Drive-like backend where users can upload and organize files, create folders, share files through public links, restore deleted files, and manage their storage securely.

## ✨ Highlights

- 🔐 JWT Authentication & Refresh Token Rotation
- 📁 File & Folder Management
- ☁️ MinIO Object Storage
- 🔗 Public Share Links
- 🗑️ Trash & Restore System
- ⚡ Resilience4j Circuit Breaker & Retry
- 🛡️ Role-Based Authorization
- 📊 Actuator & Micrometer Metrics
- 🧪 Unit & Integration Tests
- 🐳 Docker & Docker Compose
- 🔄 GitHub Actions CI

## 🛠 Tech Stack

| Category | Technologies |
|---|---|
| Backend | Java 17, Spring Boot |
| Security | Spring Security, JWT, BCrypt |
| Database | PostgreSQL, Spring Data JPA, Hibernate |
| Storage | MinIO |
| Resilience | Resilience4j |
| Database Migration | Flyway |
| Monitoring | Actuator, Micrometer |
| API Documentation | Swagger / OpenAPI |
| Testing | JUnit 5, Mockito, Testcontainers |
| DevOps | Maven, Docker, GitHub Actions |

## 🏗 Architecture

```text
                    ┌─────────────────┐
                    │   REST Client   │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   Controller    │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │     Service     │
                    └───────┬─────────┘
                            │
                 ┌──────────┴──────────┐
                 ▼                     ▼
        ┌────────────────┐    ┌────────────────┐
        │   PostgreSQL   │    │     MinIO      │
        │    Metadata    │    │  File Storage  │
        └────────────────┘    └────────────────┘
```

## 🔐 Security

The application includes:

- Short-lived JWT access tokens
- Refresh token rotation
- SHA-256 hashed refresh tokens
- Token family reuse detection
- BCrypt password hashing
- USER / ADMIN authorization
- Resource ownership validation
- Login rate limiting by username and IP
- Request validation with Bean Validation

## 📦 File Management

Users can:

- Upload and download files
- Create hierarchical folders
- Move deleted files to trash
- Restore deleted files
- Generate temporary presigned download URLs
- Create public sharing links with `VIEW` or `DOWNLOAD` permission

File metadata is stored in **PostgreSQL**, while file content is stored in **MinIO**.

## 🗑 Trash & Cleanup

Files are not permanently removed immediately.

When a file is deleted:

```text
Delete Request
      ↓
Soft Delete
      ↓
Trash
      ↓
Scheduled Cleanup
      ↓
Permanent MinIO Deletion
```

This allows deleted files to be restored before the retention period expires.

## ⚡ Resilience

MinIO operations are protected with **Resilience4j**.

- Circuit Breaker prevents repeated calls when MinIO is unavailable.
- Retry is used for suitable idempotent operations.
- Upload operations are not automatically retried because the input stream may already be consumed.
- HTTP connection and read/write timeouts are configured separately.

## 📖 API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
```

JWT-protected endpoints can be tested using the **Authorize** button.

## 🧪 Testing

Run unit tests:

```bash
mvn test
```

Run integration tests:

```bash
mvn verify -Pintegration-tests
```

Integration tests use **Testcontainers** with real PostgreSQL and MinIO containers.

## 📊 Monitoring

Available Actuator endpoints include:

```text
/actuator/health
/actuator/metrics
/actuator/prometheus
/actuator/circuitbreakers
```

The application also includes a custom MinIO health indicator and Micrometer counters for file operations.

## 🐳 Docker

Start the complete environment with:

```bash
docker compose up --build
```

This starts:

```text
Spring Boot Application
PostgreSQL
MinIO
```

## 🚀 Running Locally

### Requirements

- Java 17
- Maven
- PostgreSQL
- MinIO

Set the required environment variables:

```text
JWT_SECRET
```

Then run:

```bash
mvn spring-boot:run
```

## 📌 Main Endpoints

| Endpoint | Description |
|---|---|
| `/api/auth` | Authentication & token operations |
| `/api/files` | File operations |
| `/api/folders` | Folder management |
| `/api/share` | Public file access |
| `/api/share-links` | Share link management |
| `/api/admin` | Admin operations |

## ⚠️ Current Limitations

- Recursive folder deletion is not implemented.
- File versioning is not implemented.
- Login rate limiting is currently in-memory.
- Access tokens remain valid until their expiration after logout.
- Circuit breaker state transitions do not have a dedicated integration test.

## 🔄 CI

GitHub Actions automatically runs tests and builds the application on pushes and pull requests.