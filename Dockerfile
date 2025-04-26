FROM ubuntu:latest
LABEL authors="goni"

ENTRYPOINT ["top", "-b"]

# 1. Use an official OpenJDK runtime as the base image
FROM openjdk:21-jdk-slim

# 2. Set the working directory inside the container
WORKDIR /app

# 3. Copy the built JAR file into the container
# Make sure to replace 'javame-trans-service.jar' with your actual Spring Boot JAR file name
COPY target/*.jar javame-trans-service.jar

# 4. Expose the port your Spring Boot app runs on
EXPOSE 10284

# 5. Run the Spring Boot JAR file
CMD ["java", "-jar", "javame-trans-service.jar"]