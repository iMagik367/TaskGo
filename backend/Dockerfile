# Use OpenJDK 17
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy gradle files
COPY gradle/ gradle/
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle.kts .
COPY build.gradle.kts .

# Copy backend source
COPY backend/ backend/

# Make gradlew executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew :backend:build --no-daemon

# Expose port
EXPOSE 8080

# Set environment variables
ENV PORT=8080
ENV DB_ENABLE=true

# Run the application
CMD ["./gradlew", ":backend:run", "--no-daemon"]