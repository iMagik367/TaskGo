FROM gradle:8.7-jdk17 AS build
WORKDIR /workspace
COPY . /workspace
RUN chmod +x ./gradlew
RUN ./gradlew :backend:installDist --no-daemon

FROM eclipse-temurin:17-jre
ENV JAVA_OPTS="-Xms128m -Xmx512m"
WORKDIR /app
COPY --from=build /workspace/backend/build/install/backend /app
EXPOSE 8080
CMD ["sh","-c","/app/bin/backend $JAVA_OPTS"]

