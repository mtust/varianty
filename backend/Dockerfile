# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately from source so code edits don't re-download them
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/target/*.jar app.jar
USER spring:spring

# Cloud Run injects PORT (defaults to 8080); MaxRAMPercentage keeps the JVM
# heap sized to the container memory limit instead of the host's.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
