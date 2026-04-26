# Swagger/OpenAPI Configuration Guide

## Overview

Swagger (OpenAPI 3.0) documentation has been successfully integrated into the Seedha Hissab API. This provides interactive API documentation with the ability to test endpoints directly from the browser.

## Configuration Details

### 1. Dependencies Added

Added the SpringDoc OpenAPI dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

### 2. OpenAPI Configuration

Created a new configuration class: `OpenApiConfig.java`

This class configures:
- **API Title**: Seedha Hissab API
- **Version**: 1.0.0
- **Description**: API for Seedha Hissab - Personal Finance Management System
- **Authentication**: JWT Bearer token authentication
- **Contact**: support@seedhahisaab.com

### 3. Security Configuration Updates

Updated `SecurityConfig.java` to permit access to Swagger UI endpoints:
- `/swagger-ui/**` - Swagger UI interface
- `/v3/api-docs/**` - OpenAPI specification endpoints

These endpoints are accessible without authentication.

### 4. Automatic API Documentation

The springdoc-openapi library automatically discovers and documents:
- All REST endpoints
- Request/response schemas
- Path parameters, query parameters, and request bodies
- HTTP status codes and response types

## Accessing Swagger UI

### Local Development

1. Build and run the application:
```bash
mvn clean install
mvn spring-boot:run
```

2. Open your browser and navigate to:
```
http://localhost:8080/swagger-ui.html
```

### Docker Deployment

1. Build the Docker image:
```bash
docker build -t seedha-hissab-backend:latest .
```

2. Run the container:
```bash
docker run -p 8080:8080 seedha-hissab-backend:latest
```

3. Access Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

### Production/Remote Server

Replace `localhost:8080` with your server's address:
```
http://<your-server-domain>:8080/swagger-ui.html
```

## API Documentation Endpoints

### Swagger UI
- **URL**: `/swagger-ui.html`
- **Description**: Interactive API documentation

### OpenAPI Specification (JSON)
- **URL**: `/v3/api-docs`
- **Description**: OpenAPI 3.0 specification in JSON format

### OpenAPI Specification (YAML)
- **URL**: `/v3/api-docs.yaml`
- **Description**: OpenAPI 3.0 specification in YAML format

## Testing Endpoints with Swagger UI

1. Navigate to `/swagger-ui.html`
2. All public endpoints are immediately available
3. For authenticated endpoints:
   - Click the **Authorize** button (lock icon)
   - Obtain a JWT token by calling the `/auth/login` endpoint
   - Paste the token in the Bearer Authorization dialog
   - Click "Authorize" to apply the token to all subsequent requests

## API Endpoints Overview

### Authentication
- `POST /auth/register` - Register a new user
- `POST /auth/login` - User login

### Health
- `GET /healthz` - Health check

### Projects
- `POST /projects` - Create project
- `GET /projects` - List user's projects
- `GET /projects/{id}` - Get project details
- `POST /projects/{id}/partners` - Add partner to project
- `GET /projects/{id}/partners` - Get project partners
- `GET /projects/{id}/vendors` - Get project vendors
- `GET /projects/{id}/summary` - Get project financial summary
- `GET /projects/{id}/vendors/ledger` - Get vendor ledgers
- `GET /projects/{id}/settlement` - Get settlement information

### Partners
- `GET /partners/{id}` - Get partner details
- `PUT /partners/{id}` - Update partner
- `DELETE /partners/{id}` - Delete partner

### Transactions
- `POST /transactions` - Create transaction
- `PUT /transactions/{id}` - Update transaction
- `PATCH /transactions/{id}/omit` - Omit transaction
- `GET /projects/{projectId}/transactions` - Get project transactions (paginated)
- `GET /transactions/{id}/history` - Get transaction history

### Vendors
- `POST /vendors` - Create vendor
- `GET /vendors` - List all vendors
- `GET /vendors/{id}/ledger` - Get vendor ledger

### Personal Transactions
- `POST /personal/transactions` - Create personal transaction
- `GET /personal/transactions` - List personal transactions (paginated)
- `GET /personal/summary` - Get personal transaction summary
- `GET /personal/transactions/{id}/history` - Get personal transaction history

## Configuration Properties

The API configuration can be customized in `application.properties`:

```properties
# Server port
server.port=8080

# Database configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/seedha_hissab
spring.datasource.username=postgres
spring.datasource.password=password

# CORS configuration
cors.allowed.origin=http://localhost:3000

# JWT configuration
jwt.secret=<your-jwt-secret-key>
jwt.expiration=86400000
```

## Troubleshooting

### Swagger UI not loading
- Ensure the application is running on the configured port
- Check that the `/swagger-ui/**` path is permitted in SecurityConfig
- Verify the springdoc-openapi dependency is correctly added to pom.xml

### Cannot authorize with JWT
- Make sure you have obtained a valid JWT token from `/auth/login`
- Use the exact format: `Bearer <token>` (including the "Bearer " prefix)
- Check that the token has not expired

## API Security

- All endpoints except `/auth/register`, `/auth/login`, and `/healthz` require JWT authentication
- Swagger UI and OpenAPI specification endpoints are accessible without authentication
- Consider restricting Swagger UI access in production environments by:
  - Adding additional security rules in SecurityConfig
  - Using a reverse proxy to hide Swagger endpoints
  - Running the application behind authentication middleware

## Future Enhancements

1. Add custom annotations for enhanced documentation (optional)
2. Add request/response examples for each endpoint
3. Implement schema documentation for all DTOs
4. Add rate limiting documentation
5. Implement webhooks documentation

## References

- [SpringDoc OpenAPI](https://springdoc.org/)
- [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.3)
- [Swagger UI Documentation](https://swagger.io/tools/swagger-ui/)
