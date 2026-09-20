# Dockerfile

# Build stage
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY api-service/pom.xml api-service/pom.xml
COPY sdk/pom.xml sdk/pom.xml
COPY parking-lot-common/pom.xml parking-lot-common/pom.xml

COPY mvnw .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -B -ntp dependency:go-offline

COPY parking-lot-common/src parking-lot-common/src
COPY api-service/src api-service/src

RUN ./mvnw -B -ntp -pl api-service -am clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

ADD https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip /tmp/newrelic-java.zip
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && unzip /tmp/newrelic-java.zip -d /app \
    && rm /tmp/newrelic-java.zip \
    && apt-get purge -y --auto-remove unzip \
    && rm -rf /var/lib/apt/lists/*

COPY newrelic/newrelic.yml /app/newrelic/newrelic.yml

COPY --from=build /app/api-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-javaagent:/app/newrelic/newrelic.jar", "-Dnewrelic.config.file=/app/newrelic/newrelic.yml", "-jar", "app.jar"]