# Docker Setup Guide

## Prerequisites

- Docker Desktop installed (Windows/Mac) or Docker + Docker Compose (Linux)
- Check installation: `docker --version` and `docker compose --version`

## Quick Start (Recommended)

### 1. Start Everything with Docker Compose

Run from the project root directory:

```bash
cd /home/martin/Documents/java/seeedhaHissab/seedhaHissab-backend

# Start both PostgreSQL and Spring Boot API with one command
docker compose up --build
```

This will:
- Build the Docker image for the Spring Boot application
- Start PostgreSQL 16 container
- Start the Spring Boot API container
- Both containers are ready when you see: `Tomcat started on port(s): 8080`

**Access the API:**
```
http://localhost:8080/api/healthz
http://localhost:8080/swagger-ui.html
```

**Stop the containers:**
```bash
Ctrl+C
docker compose down
```

---

### 2. Start Only PostgreSQL (Run App Locally)

Useful for debugging in IntelliJ/VS Code:

```bash
# Terminal 1: Start only the database
docker compose up db

# Terminal 2: Run the application locally
export DATABASE_URL=postgresql://seedhahisaab:seedhahisaab@localhost:5432/seedhahisaab
export SESSION_SECRET=dev-secret-key-minimum-32-characters-long-for-jwt-dev-use-only
export CORS_ALLOWED_ORIGIN=http://localhost:5173
mvn spring-boot:run
```

**Or create a `.env` file and source it:**
```bash
# Terminal 1: Start database
docker compose up db

# Terminal 2: Load .env and run
source .env
mvn spring-boot:run
```

---

## Common Docker Commands

### View running containers
```bash
docker compose ps
```

### View logs
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f app
docker compose logs -f db

# Last 100 lines
docker compose logs --tail=100
```

### Access PostgreSQL from command line
```bash
docker exec -it seedhahisaab-db psql -U seedhahisaab -d seedhahisaab

# List tables
\dt

# Exit
\q
```

### Rebuild after code changes
```bash
docker compose up --build
```

### Wipe everything and start fresh
```bash
# Stop and remove containers, networks, and volumes
docker compose down -v

# Then restart
docker compose up --build
```

### Stop containers without removing
```bash
docker compose stop

# Start them again
docker compose start
```

---

## Troubleshooting

### Port Already in Use

If port 8080 or 5432 is already in use, modify `docker-compose.yml`:

```yaml
services:
  db:
    ports:
      - "5433:5432"    # Changed from 5432:5432
  
  app:
    ports:
      - "8081:8080"    # Changed from 8080:8080
```

Then access API at: `http://localhost:8081/api`

### Database Connection Refused

1. Check if PostgreSQL container is running:
```bash
docker compose ps
```

2. Check database logs:
```bash
docker compose logs db
```

3. Wait for healthcheck to pass (takes ~5-10 seconds):
```bash
docker compose logs -f db | grep healthcheck
```

### Application Won't Start

Check the application logs:
```bash
docker compose logs app
```

Common issues:
- `DATABASE_URL` not set → Set in `.env` file
- `SESSION_SECRET` not set → Set in `.env` file
- Database not ready → Wait for healthcheck to pass
- Port 8080 in use → Change ports in docker-compose.yml

### Permission Denied Error

On Linux, if you get permission denied errors:
```bash
sudo usermod -aG docker $USER
# Log out and back in, or run:
newgrp docker
```

---

## Environment Configuration

The `.env` file controls all configuration:

```properties
# Database
POSTGRES_USER=seedhahisaab
POSTGRES_PASSWORD=seedhahisaab
POSTGRES_DB=seedhahisaab

# Application
SESSION_SECRET=your-32-char-minimum-secret
CORS_ALLOWED_ORIGIN=http://localhost:5173
PORT=8080

# Local development database URL
DATABASE_URL=postgresql://seedhahisaab:seedhahisaab@localhost:5432/seedhahisaab
```

**To use different credentials:**

1. Edit `.env` file
2. Run: `docker compose down -v` (wipe old database)
3. Run: `docker compose up --build` (start fresh)

---

## Docker Compose File Structure

```yaml
services:
  db:
    # PostgreSQL 16 database
    # Stores data in named volume (persists after container stops)
    # Healthcheck ensures app waits for DB readiness
  
  app:
    # Spring Boot application
    # Depends on db service
    # Reads configuration from .env file
    # Starts only after database is healthy
```

---

## Development Workflow

### Make code changes, rebuild, and test:

```bash
# 1. Make changes to Java code
# 2. Stop containers
docker compose down

# 3. Rebuild and start
docker compose up --build

# 4. Watch logs
docker compose logs -f app
```

### Test API with curl:

```bash
# Health check
curl http://localhost:8080/api/healthz

# Get Swagger UI
open http://localhost:8080/swagger-ui.html

# Register user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","name":"Test User"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

---

## Production Deployment

For production Docker deployment:

1. Generate a strong SESSION_SECRET:
```bash
openssl rand -base64 48
```

2. Use a .env.prod file with production secrets:
```bash
docker compose --env-file .env.prod up --build -d
```

3. Use a reverse proxy (nginx) for HTTPS
4. Never expose PostgreSQL port to internet
5. Use managed database (RDS, Azure Database) instead of container

---

## Resources

- [Docker Docs](https://docs.docker.com/)
- [Docker Compose Docs](https://docs.docker.com/compose/)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
