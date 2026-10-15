# mcp-server/Dockerfile
# Build from the repository root: docker build -f mcp-server/Dockerfile .

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

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -B -ntp dependency:go-offline

COPY mcp-server/src mcp-server/src
RUN ./mvnw -B -ntp -pl mcp-server -am clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/mcp-server/target/*.jar app.jar

# Must listen on all interfaces so the API container can reach it over the private Docker network.
# Do not publish this port on the host: the MCP server has no authentication.
ENV MCP_SERVER_ADDRESS=0.0.0.0
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
