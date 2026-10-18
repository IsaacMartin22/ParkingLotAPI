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
COPY mcp-server/pom.xml mcp-server/pom.xml

COPY mvnw .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -B -ntp dependency:go-offline

COPY parking-lot-common/src parking-lot-common/src
COPY api-service/src api-service/src
COPY mcp-server/src mcp-server/src
RUN ./mvnw -B -ntp -pl api-service,mcp-server -am clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Install yt-dlp and its runtime dependencies in the final image.
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL \
    https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp \
    -o /usr/local/bin/yt-dlp \
    && chmod 755 /usr/local/bin/yt-dlp

COPY newrelic/ /app/newrelic/
COPY --from=build /app/api-service/target/*.jar app.jar
COPY --from=build /app/mcp-server/target/*.jar mcp-server.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

RUN cd /app/newrelic && jar xf newrelic-java-9.4.0.zip && test -f /app/newrelic/newrelic.jar
RUN sed -i 's/\r$//' /app/docker-entrypoint.sh && chmod +x /app/docker-entrypoint.sh

# Only the API port is exposed; the bundled MCP server listens on 127.0.0.1:8081 inside the container.
EXPOSE 8080

ENTRYPOINT ["/app/docker-entrypoint.sh"]