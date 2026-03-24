# Stage 1: Build the application using Maven
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the final, lean image
FROM openjdk:27-ea-slim-trixie
WORKDIR /app
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT-exec.jar app-exec.jar
EXPOSE 8080
CMD ["java", "-jar", "app-exec.jar"]
