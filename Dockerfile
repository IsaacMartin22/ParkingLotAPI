# Dockerfile

# Build stage
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x ./mvnw
RUN ./mvnw -B -ntp dependency:go-offline

COPY src src

RUN ./mvnw -B -ntp clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
