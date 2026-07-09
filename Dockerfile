# Build Stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
# (Optional) RUN mvn dependency:go-offline to cache dependencies
COPY src ./src
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/batterymantra.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
