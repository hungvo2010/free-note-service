# Build stage
FROM gradle:jdk21 AS build
WORKDIR /app
COPY . .
RUN ./gradlew :free-draw:shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/free-draw/build/libs/*.jar app.jar
COPY keystore.p12 /app/keystore.p12
ENV SSL_PORT=8189
ENV ALLOWED_ORIGINS="https://free-note-ui.vercel.app" 
EXPOSE 8189
ENTRYPOINT ["java", "-jar", "app.jar"]
