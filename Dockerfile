FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN mkdir -p /var/www/batterymantra/uploads
COPY --from=build /app/target/batterymantra.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
