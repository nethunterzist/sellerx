# Backend Dockerfile - Multi-environment support
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first for better caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
# Profile will be set via environment variable at runtime
# Default to docker profile for local development
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar
COPY --from=build /app/target/*.jar app.jar

# Default profile for Docker (can be overridden)
ENV SPRING_PROFILES_ACTIVE=docker

# Railway uses PORT environment variable, Docker uses 8080
EXPOSE ${PORT:-8080}

# Run the application with profile from environment variable
CMD ["sh", "-c", "java -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -Dserver.port=${PORT:-8080} -jar app.jar"]
