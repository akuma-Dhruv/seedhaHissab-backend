# SeedhaHisaab — Backend (Spring Boot + PostgreSQL)

REST API for the SeedhaHisaab transaction-based financial management app.

- **Stack:** Java 17 · Spring Boot 3.2 · Spring Data JPA · Spring Security · JWT · PostgreSQL
- **Frontend repo:** https://github.com/akuma-Dhruv/seedhaHissab-Frontend

---

## Quick start (Docker — easiest)

You need [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed.

```bash
# 1. Clone and enter the project
git clone https://github.com/akuma-Dhruv/seedhaHissab-backend.git
cd seedhaHissab-backend

# 2. Create your env file from the template and edit SESSION_SECRET
cp .env.example .env
#   -> open .env in any editor and set a long random SESSION_SECRET

# 3. Start everything (Postgres + the API)
docker compose up --build
```

The API will be available at **http://localhost:8080/api**.

To stop: `Ctrl+C`, then `docker compose down`.
To wipe the database completely: `docker compose down -v`.

---

## Quick start (only DB in Docker, app run locally)

Useful if you want to debug the app from IntelliJ / VS Code.

```bash
# Start ONLY the Postgres container
docker compose up db

# In a second terminal, export the env vars and run the app with Maven
export DATABASE_URL=postgresql://seedhahisaab:seedhahisaab@localhost:5432/seedhahisaab
export SESSION_SECRET=replace-me-with-a-long-random-string
export CORS_ALLOWED_ORIGIN=http://localhost:5173
mvn spring-boot:run
```

---

## Quick start (no Docker at all)

You'll need:
- JDK 17+
- Maven 3.9+
- A running PostgreSQL 14+ instance

```bash
# 1. Create a database
createdb seedhahisaab

# 2. Export env vars
export DATABASE_URL=postgresql://<user>:<password>@localhost:5432/seedhahisaab
export SESSION_SECRET=replace-me-with-a-long-random-string
export CORS_ALLOWED_ORIGIN=http://localhost:5173

# 3. Run
mvn spring-boot:run
```

---

## Environment variables

| Variable              | Required | Default                    | Description                                           |
| --------------------- | -------- | -------------------------- | ----------------------------------------------------- |
| `DATABASE_URL`        | yes      | —                          | `postgresql://user:pass@host:port/db`                 |
| `SESSION_SECRET`      | yes      | —                          | Long random string used to sign JWTs                  |
| `CORS_ALLOWED_ORIGIN` | no       | (none — CORS disabled)     | The exact frontend origin, e.g. `http://localhost:5173` |
| `PORT`                | no       | `8080`                     | Port the server listens on                            |

The Docker setup also uses `POSTGRES_USER`, `POSTGRES_PASSWORD`, and
`POSTGRES_DB` to seed the DB container. See `.env.example`.

---

## Project layout

```
.
├── Dockerfile              # multi-stage build (Maven build -> JRE runtime)
├── docker-compose.yml      # Postgres + (optional) Spring Boot app
├── .env.example            # template for environment variables
├── pom.xml                 # Maven dependencies
└── src/
    └── main/
        ├── java/com/seedhahisaab/
        │   ├── config/     # DataSource, Security, CORS
        │   ├── controller/ # REST endpoints
        │   ├── service/    # business logic
        │   ├── repository/ # JPA repositories
        │   ├── entity/     # JPA entities
        │   └── security/   # JWT util, filters
        └── resources/
            └── application.properties
```

---

## Useful commands

```bash
# Run the test suite
mvn test

# Build a runnable JAR (output: target/api-server-0.0.1-SNAPSHOT.jar)
mvn clean package

# Tail the app container logs
docker compose logs -f app

# Open a psql shell inside the DB container
docker compose exec db psql -U seedhahisaab -d seedhahisaab
```

---

## Notes

- JPA is configured with `spring.jpa.hibernate.ddl-auto=update`, so schema
  changes are auto-applied on startup. For production you should switch this
  to `validate` and use Flyway/Liquibase migrations.
- CORS is locked down to `CORS_ALLOWED_ORIGIN`. Set this correctly or browser
  requests from the frontend will fail.
