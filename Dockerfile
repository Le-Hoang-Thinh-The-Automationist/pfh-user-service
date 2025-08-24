# Use a lightweight OpenJDK base image
FROM openjdk:17-jdk-slim

# Copy the jar file into the container
COPY target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]