# 🚀 Quick Start Guide - Docker Setup

## ✅ All Set! Here's what to do now:

### Step 1: Navigate to the project
```bash
cd /home/martin/Documents/java/seeedhaHissab/seedhaHissab-backend
```

### Step 2: Everything is already configured
Your `.env` file is ready with:
- ✅ Database credentials (PostgreSQL)
- ✅ JWT Secret (development)
- ✅ CORS settings for localhost
- ✅ Database URL for local development

### Step 3: Start Everything with Docker

**Option A: Full Stack (PostgreSQL + API) [RECOMMENDED]**
```bash
docker compose up --build
```

This starts:
- PostgreSQL database on port 5432
- Spring Boot API on port 8080

Wait for output showing: `Tomcat started on port(s): 8080`

**Then access:**
- 🌐 Swagger UI: http://localhost:8080/swagger-ui.html
- 📡 Health Check: http://localhost:8080/api/healthz
- 📖 API Docs (JSON): http://localhost:8080/api/v3/api-docs

---

**Option B: Only Database (Run API locally in IDE)**
```bash
# Terminal 1
docker compose up db

# Terminal 2 (in IDE - use VS Code debugger)
export $(cat .env | xargs)
mvn spring-boot:run
```

---

## 🛑 Stop Everything

```bash
# Press Ctrl+C in the terminal where docker compose is running
# Or run:
docker compose down
```

---

## 🗑️ Clean Up Database

To reset the database completely:
```bash
docker compose down -v
docker compose up --build
```

---

## 📊 Check Status

```bash
# See running containers
docker compose ps

# View logs
docker compose logs -f

# See database logs only
docker compose logs -f db

# See API logs only
docker compose logs -f app
```

---

## 🧪 Test the API

### Health Check
```bash
curl http://localhost:8080/api/healthz
```

### Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "name": "Test User"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

---

## ⚙️ Configuration Files

- `.env` - Environment variables (database, JWT, CORS)
- `application.properties` - Spring Boot configuration
- `docker-compose.yml` - Docker services definition
- `Dockerfile` - Docker image build instructions

## 📖 Detailed Documentation

- `DOCKER_SETUP.md` - Complete Docker guide
- `JWT_SETUP.md` - JWT configuration guide
- `SWAGGER_SETUP.md` - Swagger/OpenAPI setup guide
- `README.md` - Project overview

---

## 🎯 Next Steps

1. **Run Docker**: `docker compose up --build`
2. **Test API**: http://localhost:8080/swagger-ui.html
3. **Check Logs**: `docker compose logs -f`
4. **Read Docs**: See documentation files above

---

## ❌ Troubleshooting

**Port already in use?**
```bash
# Change ports in docker-compose.yml, then:
docker compose up --build
```

**Database won't connect?**
```bash
# Check database logs
docker compose logs db

# Reset database
docker compose down -v
docker compose up --build
```

**Need to rebuild after code changes?**
```bash
docker compose up --build
```

---

## 📝 Notes

- The `.env` file is git-ignored for security
- Database persists in named volume (survives container restart)
- PostgreSQL runs in the background
- API auto-creates database schema on startup
- Logs show real-time application output

---

**Questions?** Check the detailed documentation files in the project root directory.
