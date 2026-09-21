# AI PDF Service

---

### Table of Contents

-  [Description](#description)
-  [Installation](#installation)
-  [Testing](#testing)

---

## Description

A **PDF processing backend** built with Java and Spring Boot. Users authenticate with JWT, upload PDFs to Amazon S3, and run AI-assisted and iText operations on their files.

- **User Management:** Registration and login with Spring Security and JWT; admin endpoints for user and audit overview;
- **PDF Management:** Upload, list, fetch, and delete PDF files; files are stored in S3 and metadata in PostgreSQL;
- **Processing:** AI-guided redaction, AES-256 encryption / permissions, summarization, and compression;
- **Jobs & Audit:** Processing job records and user-scoped audit logs with pagination and filters;
- **Persistence:** PostgreSQL with Spring Data JPA / Hibernate; JSONB for encryption settings.

**Technical stack:** Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA, PostgreSQL, MapStruct, Lombok, AWS S3, OpenAI, iText, Docker, Swagger, JUnit.

---

## Installation

1️⃣ Clone the repository to your local directory:

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

The API is served at **http://localhost:8080/api/v1**. Postgres is published on **localhost:5555** if you need a local SQL client.

---

## Testing

Once the app is running, open the **API documentation** in **Swagger UI**:

**http://localhost:8080/api/v1/swagger-ui/index.html**

Authorize with a JWT from `POST /auth/signup` or `POST /auth/login` (`Authorization: Bearer <token>`). Admin routes require a user with the `ADMIN` role.
