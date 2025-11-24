# Stage 1: Build the application using a Gradle image
FROM gradle:jdk17 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean build --no-daemon

# Stage 2: Create the final, lightweight runtime image
FROM eclipse-temurin:17-jre-focal-slim
WORKDIR /app
# Copy the built JAR file from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080
