# =============================================================================
# SeedhaHisaab Backend - Dockerfile
# -----------------------------------------------------------------------------
# Multi-stage build:
#   Stage 1 ("build")  -> uses a Maven + JDK 17 image to compile the project
#                         and produce a fat JAR in /workspace/app/target/
#   Stage 2 ("runtime")-> uses a small JRE-only image and copies in just the
#                         JAR. Final image is small and contains no build tools.
#
# Build the image manually:
#     docker build -t seedhahisaab-backend .
#
# Or just run `docker compose up` from this folder (recommended).
# =============================================================================

# ---------- Stage 1: Build ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build

# Working directory inside the build container
WORKDIR /workspace/app

# Copy the Maven descriptor first so Docker can cache the dependency
# download layer. As long as pom.xml does not change, `mvn dependency:go-offline`
# will be served from cache on subsequent builds.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Now copy the actual source code and build the executable JAR.
# -DskipTests keeps the image build fast; run tests in CI separately.
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:17-jre-jammy

# A non-root user is safer than running the JVM as root.
RUN useradd --system --create-home --shell /usr/sbin/nologin spring
USER spring
WORKDIR /home/spring

# Copy ONLY the built JAR from the build stage (rename it to app.jar for clarity).
COPY --from=build /workspace/app/target/*.jar app.jar

# Spring Boot will read PORT from the env (see application.properties).
# Default to 8080 if PORT is not provided.
ENV PORT=8080
EXPOSE 8080

# Use exec form so signals (SIGTERM from `docker stop`) reach the JVM directly
# and Spring Boot can shut down gracefully.
ENTRYPOINT ["java", "-jar", "/home/spring/app.jar"]
