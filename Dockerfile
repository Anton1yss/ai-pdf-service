FROM gradle:8.13-jdk21-alpine AS builder
WORKDIR /docker-build/
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon
COPY src ./src/
RUN gradle build -x test --no-daemon

# RUNTIME STAGE
FROM openjdk:25-ea-21-slim
COPY --from=builder /docker-build/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]