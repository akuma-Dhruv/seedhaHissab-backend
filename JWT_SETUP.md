# JWT Configuration Guide

## Overview

The Seedha Hissab API uses JWT (JSON Web Tokens) for authentication. The JWT secret is used to sign and verify tokens.

## Configuration Options

### 1. Development Environment (Automatic)

For development, the application will automatically use a default JWT secret if none is configured. You'll see a warning message:

```
⚠️  WARNING: Using default JWT secret for development. Set SESSION_SECRET environment variable or jwt.secret property for production.
```

This allows you to run the application immediately without configuration.

### 2. Development with Custom Secret

#### Option A: Environment Variable
```bash
export SESSION_SECRET="your-32-character-minimum-secret-key"
mvn spring-boot:run
```

#### Option B: Application Properties
Add to `src/main/resources/application.properties`:
```properties
jwt.secret=your-32-character-minimum-secret-key
```

#### Option C: .env File (Docker Compose)
1. Copy the example file:
```bash
cp .env.example .env
```

2. Edit `.env` and set:
```
SESSION_SECRET=your-32-character-minimum-secret-key
```

3. Run with Docker Compose:
```bash
docker-compose up
```

### 3. Production Environment (Required)

For production, you **must** set the `SESSION_SECRET` environment variable or the application will fail to start.

#### Generate a Secure Secret

Use OpenSSL to generate a strong random secret:

```bash
openssl rand -base64 48
```

This produces a base64-encoded random string with high entropy.

Example output:
```
aBcDeFgHiJkLmNoPqRsTuVwXyZaBcDeFgHiJkLmNoPqRsTuVwXyZaBcDeFgHiJk
```

#### Set Environment Variable

```bash
export SESSION_SECRET="aBcDeFgHiJkLmNoPqRsTuVwXyZaBcDeFgHiJkLmNoPqRsTuVwXyZaBcDeFgHiJk"
```

Or in your deployment platform (Docker, Kubernetes, Cloud Run, etc.):

**Docker:**
```bash
docker run -e SESSION_SECRET="your-secret-key" seedha-hissab-backend:latest
```

**Kubernetes Secret:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: jwt-secret
type: Opaque
stringData:
  SESSION_SECRET: your-secret-key
---
apiVersion: v1
kind: Pod
metadata:
  name: seedha-hissab-backend
spec:
  containers:
  - name: backend
    image: seedha-hissab-backend:latest
    env:
    - name: SESSION_SECRET
      valueFrom:
        secretKeyRef:
          name: jwt-secret
          key: SESSION_SECRET
```

**Azure Container Instances:**
```bash
az container create \
  --resource-group myResourceGroup \
  --name seedha-hissab-backend \
  --image seedha-hissab-backend:latest \
  --environment-variables SESSION_SECRET="your-secret-key"
```

## Token Configuration

### Token Expiration

The default token expiration is **24 hours** (86400000 milliseconds).

Configure in `application.properties`:
```properties
jwt.expiration.ms=86400000
```

Or set environment variable:
```bash
export JWT_EXPIRATION_MS=86400000
```

## Testing

### Testing with Swagger UI

1. Start the application
2. Navigate to `http://localhost:8080/swagger-ui.html`
3. Call `/auth/login` to get a JWT token
4. Click **Authorize** button (lock icon)
5. Enter the token in format: `Bearer <token>`
6. Click **Authorize** to use the token for all subsequent requests

### Testing with cURL

1. Get a token:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password"}'
```

2. Use the token:
```bash
curl -X GET http://localhost:8080/api/projects \
  -H "Authorization: Bearer <token>"
```

## Security Best Practices

1. **Never commit secrets to git**
   - `.env` file is in `.gitignore`
   - Never hardcode secrets in code

2. **Use strong random secrets**
   - Minimum 32 characters
   - Use `openssl rand -base64 48` to generate
   - Different secret per environment

3. **Rotate secrets regularly**
   - Change production secrets periodically
   - Implement key rotation in enterprise deployments

4. **Secure transmission**
   - Always use HTTPS in production
   - Never send tokens in URL (use Authorization header)

5. **Token expiration**
   - Use short expiration times (24 hours or less)
   - Implement refresh token mechanism for long-lived sessions

## Troubleshooting

### Error: "SESSION_SECRET environment variable is required in production"

**Cause:** Application is running in production mode without JWT secret configured.

**Solution:**
1. Generate a new secret: `openssl rand -base64 48`
2. Set the environment variable: `export SESSION_SECRET="your-secret"`
3. Or add to `.env` file and source it before starting
4. Restart the application

### Error: "SESSION_SECRET must be at least 32 characters long"

**Cause:** The provided secret is too short.

**Solution:**
- Use `openssl rand -base64 48` to generate a proper length secret
- Ensure the secret is at least 32 characters

### Tokens not being validated

**Cause:** Different applications using different secrets, or secret changed between token creation and validation.

**Solution:**
- Ensure all instances use the same `SESSION_SECRET`
- If you rotated the secret, existing tokens will become invalid
- Users will need to login again to get new tokens

## References

- [JWT.io](https://jwt.io/) - JWT debugging and specification
- [JJWT Documentation](https://github.com/jwtk/jjwt) - Java JWT library used in this project
- [OWASP - Token Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
