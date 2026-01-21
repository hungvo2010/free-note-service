# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :free-draw:shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/free-draw/build/libs/*-all.jar app.jar
EXPOSE 8189
ENTRYPOINT ["java", "-jar", "app.jar"]
