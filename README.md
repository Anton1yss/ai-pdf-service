# AI PDF Service

A **PDF processing backend** built with Java and Spring Boot. Users authenticate with JWT, upload PDFs to Amazon S3, and run AI-assisted and iText-powered operations on their files – redaction, encryption, and summarization – with every action tracked as a processing job and logged in an audit trail.

---

### Table of Contents

- [Description](#description)
- [Tech Stack](#tech-stack)
- [Features](#features)
- [Installation](#installation)
- [Testing](#testing)
- [Project Structure](#project-structure)

---

## Description

AI PDF Service is a REST API for managing and processing PDF documents:

- **User Management** – registration and login secured with Spring Security and JWT; admin endpoints for user and audit overview.
- **PDF Management** – upload, list, fetch, and delete PDF files; files are stored in S3, with metadata kept in PostgreSQL.
- **AI-Assisted Processing** – natural-language prompts are sent to OpenAI (GPT-4o mini) to detect phrases to redact or to generate a document summary, which iText then applies to the PDF.
- **Encryption** – AES-256 encryption with configurable owner/user permissions (printing, copying, modification, etc.) via iText.
- **Jobs & Audit** – every processing action (redact, summarize, encrypt, decrypt) creates a `ProcessingJob` record, and every request is written to a user-scoped, filterable, paginated `AuditLog`.
- **Docs** – interactive API documentation via Swagger UI.

### PDF File Operations

Each PDF goes through the same lifecycle: **upload → (optionally) process → download**. All files live in S3 (metadata + processing history in Postgres), and every operation is exposed as a REST endpoint under `/pdfFile/{fileId}`.

| Operation | What happens |
|---|---|
| **Upload** | `POST /pdfFile` accepts a multipart file. It's validated before storage: must be `.pdf` / `application/pdf`, non-empty, and ≤ 10 MB. The file is pushed to S3 under an `originals/` key and a `PDFFile` record (version `1`) is created. |
| **List / Fetch** | `GET /pdfFile/my` returns a paginated list of the current user's files. `GET /pdfFile/{fileId}` returns full details, including a **pre-signed S3 URL** to download that specific version. |
| **AI Redaction** | `PUT /pdfFile/{fileId}/redact` takes a plain-language `prompt` (e.g. *"hide names and phone numbers"*). The text is extracted from the PDF, sent to OpenAI which returns the exact phrases to redact, and iText then locates every occurrence of each phrase on every page and permanently blacks it out (via `PdfCleanUpTool`, not just visual overlay). |
| **Encryption** | `PUT /pdfFile/{fileId}/encrypt` applies **AES-256** standard encryption with separate user/owner passwords, plus granular permission flags: printing, copying, content/annotation modification, form fill-in, screen-reader access, and document assembly. |
| **Summarization** | `PUT /pdfFile/{fileId}/summarize` extracts the PDF's text and asks OpenAI for a summary shaped by the given `prompt` (detail level, focus, etc.). Unlike redact/encrypt, this doesn't alter the file – it just returns the summary text. |
| **Delete** | `DELETE /pdfFile/{fileId}` removes the file from both S3 and the database. |

**Versioning:** redaction and encryption never overwrite a file in place – each run creates a *new* `PDFFile` row (`version = parent.version + 1`, linked via `parentFile`) pointing at a new S3 object, while the original stays intact. This means you always have a full, retrievable history of every transformation applied to a document.

## Tech Stack

| Category | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 3.4 (Web, Security, Validation, Data JPA) |
| Database | PostgreSQL |
| Auth | JWT (`jjwt`) |
| Storage | Amazon S3 (AWS SDK v2) |
| AI | OpenAI Java SDK (GPT-4o mini) |
| PDF Processing | iText 8 (core, cleanup, bouncy-castle-adapter) |
| Mapping | MapStruct, Lombok |
| Docs | springdoc-openapi / Swagger UI |
| Build / Deploy | Gradle, Docker, Docker Compose |
| Testing | JUnit 5 |

## Features

- **Auth:** signup and login with JWT issuance (`/auth/signup`, `/auth/login`).
- **PDF files:** upload, fetch by ID, list your own files (paginated), delete.
- **Redaction:** describe in plain English what to redact – the AI service extracts the exact phrases and iText hides them in the document.
- **Summarization:** ask for a summary with an optional focus/detail level; the AI service returns structured plain text.
- **Encryption:** apply AES-256 encryption with fine-grained owner/user permissions.
- **Processing jobs:** every redact/summarize/encrypt/decrypt action is recorded with its status, and can be looked up by job ID or by file.
- **Audit log:** every request is logged per user, with admin-only endpoints to search across all users, by user, or by file.
- **Admin panel (API):** list/inspect users and their files, and browse the full audit log.

## Installation

1️⃣ Clone the repository:

```bash
git clone https://github.com/anton1yss/ai-pdf-service.git
```

2️⃣ Navigate to the project directory:

```bash
cd ai-pdf-service
```

3️⃣ Create a `.env` file from the example and fill in real values:

```bash
cp .env.example .env
```

For Docker Compose, `POSTGRES_HOST` must be the database service name (`db`). The app connects to PostgreSQL on port **5432** inside the network. `JWT_SECRET` must be a **Base64-encoded** key (at least 256 bits for HS256). `JWT_EXPIRATION` is in milliseconds (for example `86400000` for 24 hours). You also need a valid AWS S3 bucket and an OpenAI API key.

4️⃣ Start the application:

```bash
docker compose up --build
```

❌ Stop the application:

```bash
docker compose down
```

The API is served at **http://localhost:8080/api/v1**. PostgreSQL is published on **localhost:5555** if you need a local SQL client.

> Prefer running without Docker? Use `./gradlew bootRun` after exporting the same variables (or keeping the `.env` file – it's auto-loaded via `spring.config.import`) and pointing `POSTGRES_HOST` at a reachable Postgres instance.

## Testing

Once the app is running, open the interactive **Swagger UI**:

**http://localhost:8080/api/v1/swagger-ui/index.html**

Authorize with a JWT from `POST /auth/signup` or `POST /auth/login` (`Authorization: Bearer <token>`). Admin routes require a user with the `ADMIN` role.

Unit/integration tests are run with:

```bash
./gradlew test
```

## Project Structure

```
src/main/java/by/AntonDemchuk/ai_pdf_service/
├── config/         # Security, S3, and global exception-handling configuration
├── contoller/      # REST controllers (auth, user, pdfFile, processingJob, auditLog, admin)
├── dto/            # Request/response DTOs
├── entity/         # JPA entities and enums
├── exception/      # Custom exceptions
├── filter/         # JWT authentication filter
├── mapper/         # MapStruct entity <-> DTO mappers
├── repository/     # Spring Data repositories (incl. audit log specifications)
└── service/        # Business logic: auth, users, files, AI, S3, processing jobs, audit
```