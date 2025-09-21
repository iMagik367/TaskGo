# Build stage
FROM gradle:7-jdk17 AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew :backend:build --no-daemon

# Run stage
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/backend/build/libs/*.jar app.jar
COPY --from=build /app/backend/src/main/resources/db/migration /app/db/migration

# Add health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/health || exit 1

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
