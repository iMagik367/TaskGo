FROM gradle:8.7-jdk17 AS build
WORKDIR /workspace
COPY . /workspace/server/app-server
WORKDIR /workspace/server/app-server
RUN gradle installDist --no-daemon

FROM eclipse-temurin:17-jre
ENV JAVA_OPTS="-Xms128m -Xmx512m"
WORKDIR /app
COPY --from=build /workspace/server/app-server/build/install/app-server /app
EXPOSE 8080
CMD ["sh","-c","/app/bin/app-server $JAVA_OPTS"]


