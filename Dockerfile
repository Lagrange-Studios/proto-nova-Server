FROM eclipse-temurin:21-jre
WORKDIR /app
# Run `gradle build` before building this image from the server project directory.
COPY build/libs/proto-nova-Server-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080
