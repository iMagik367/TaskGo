# Use OpenJDK 17
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy everything at once to avoid file not found issues
COPY . .

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
