# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :free-draw:shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/free-draw/build/libs/*.jar app.jar
COPY keystore.p12 /app/keystore.p12
ENV KEYSTORE_PATH=/app/keystore.p12
ENV KEYSTORE_PASSWORD=changeit
ENV SSL_PORT=8189
EXPOSE 8189
ENTRYPOINT ["java", "-jar", "app.jar"]
