# API-Service

This is a publicly available API. Currently has no authentication or validation on who can access it.
I may modify some endpoints to add authentication later, but for now it's intended to be completely open to everyone

Uses Maven multi-module build. New version publishing should be triggered only via the Buildkite pipeline on the main branch.
There is the api-service module which is the main application, there is an sdk module which is the sdk for the API service, 
and a shared common module which contains code used in both the api-service and sdk modules.
The `mcp-server` module is a separate HTTP service exposing tools to the chatbot.

The common and SDK modules are published to Maven to be added as dependencies in other Java projects. The API service is built as a Docker image and hosted 24/7 because it powers my portfolio site on GitHub Pages. Buildkite publishes the image and can optionally deploy it to DigitalOcean.
The hosted URL is currently hardcoded into both the SDK and the frontend query, so if I ever change where it's hosted I will
need to update those two places.

The repository's container workflow is centered on the root `Dockerfile`, which bundles the API service and the MCP server into one image. There is no separate Docker Compose setup to maintain.

API documentation is available at the hosted URL, which should be listed on the GitHub repository page in the upper right corner.
It lists the endpoints available and payload shapes for those endpoints.

## Java SDK

A standalone SDK module is available in `sdk/` with an API client and request/response DTOs for easier integration from other Java projects. See `sdk/README.md` for usage.
If a new SDK is to be published use the Buildkite pipeline, make sure the version is correct in pom.xml, and unblock the sdk release step.

## MCP server

The standalone `mcp-server` module uses Java 17 and the MCP Java SDK's HTTP/SSE transport
without changing the existing Spring Boot version. It does not require MongoDB or an OpenAI key.
Its tools are registered in `mcp-server\src\main\java\parkinglot\mcp\McpServerConfiguration.java`:

- `get_site_analytics` (`SiteAnalyticsTool.java`) reads the API database's `analytics` table and returns
  aggregate statistics only: total events, unique sessions, first/last event times, and optional counts grouped by
  `eventType`, `currentUrl`, `browser`, `operatingSystem`, or `day`. Optional filters are `startTime`, `endTime`,
  `eventType`, and `urlContains`. IP addresses, session ids, and payloads are never returned.
- `tool_two` is still a placeholder that returns an MCP error result until its contract is supplied.

The analytics tool uses the same PostgreSQL database as the API service, configured by the same
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` variables with the same
local defaults. Connections are opened read-only
(Hikari `read-only` plus pgjdbc `readOnlyMode=always`) and the tool only issues `SELECT` statements. In deployed
environments, you can additionally point the MCP server's `SPRING_DATASOURCE_USERNAME`/`PASSWORD` at a role limited
to `SELECT` on `analytics`, for example:

```sql
CREATE ROLE mcp_analytics_reader LOGIN PASSWORD '<secret>';
GRANT CONNECT ON DATABASE "api-service" TO mcp_analytics_reader;
GRANT USAGE ON SCHEMA public TO mcp_analytics_reader;
GRANT SELECT ON analytics TO mcp_analytics_reader;
```

Start it separately from the repository root:

```powershell
.\mvnw.cmd -pl mcp-server spring-boot:run
```

It binds to `127.0.0.1:8081` by default (`MCP_SERVER_ADDRESS` / `MCP_SERVER_PORT` override this).
Clients connect to `GET /mcp/sse`; protocol messages use `POST /mcp/messages`.
There is no authentication in this scaffold: keep it local or on a trusted private network.
Add authentication before exposing it publicly or implementing sensitive tools.

The API's MCP integration is always on. The API connects to `CHAT_MCP_BASE_URL` (default `http://localhost:8081`)
at startup and fails startup if the MCP server is unavailable, so when running locally start the MCP server first.
`ChatServiceImpl` discovers the tools and lets the model select them using `tool_choice=auto`,
then sends their results back for the final answer, with at most four tool-calling rounds.
The existing portfolio retrieval remains in place; empty retrieval can proceed to tools.
Chat answers are neither stored nor reused (the `chat_interactions` table is no longer written), so tool-backed
answers such as analytics are never stale.
Tool or transport failures follow the chatbot's existing logged error-response path.

### OpenAI model compatibility checks

`OpenAiChatModel` is the application's supported-model list, not a live OpenAI catalog.
Both the offline request tests and the opt-in live tests iterate over every enum value,
so adding a model automatically adds coverage. With tools, `gpt-6.1-sol` explicitly uses
`reasoning_effort=none` for Chat Completions; other models retain their defaults.

Run the offline checks from the repository root (no credentials or running services required):

```powershell
.\mvnw.cmd -pl api-service -am "-Dtest=ChatMcpToolsTest,ChatServiceImplTest,OpenAiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

To run live OpenAI checks, make `OPENAI_API_KEY` available in the process environment,
then explicitly enable them:

```powershell
.\mvnw.cmd -pl api-service -am "-Dtest=OpenAiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dopenai.integration=true" test
```

Live checks incur API charges and send only synthetic prompts and tool results to OpenAI.
They check each configured model's availability with `GET /v1/models/{id}`, then exercise
the production Chat Completions code for a plain answer and a full tool-call/result/answer
round trip. MCP is mocked; PostgreSQL, MongoDB, and the MCP server are not required.
Missing credentials, unavailable models, unsupported request parameters, and tool-flow
failures fail the live run rather than being silently skipped. Without the opt-in flag,
live tests are skipped even if a key exists. Run them before deploying model-list changes;
a successful model lookup alone does not establish Chat Completions or tool compatibility.

### Docker and deployment

The root `Dockerfile` builds a single image that contains both the API service and the MCP server.
`docker-entrypoint.sh` starts the MCP server first on `127.0.0.1:8081`, waits for its port to open, then starts the
API with `CHAT_MCP_BASE_URL=http://127.0.0.1:8081`. Only port 8080 is exposed, so the
MCP server is never reachable from outside the container. If either process exits, the entrypoint stops the other
and the container exits, so ECS (or Docker's restart policy) replaces it. Both processes share the container's
`SPRING_DATASOURCE_*` settings, so no new environment variables are required.

Runtime knobs (all optional):

- `MCP_JAVA_OPTS`: MCP server JVM options (default `-XX:MaxRAMPercentage=15 -XX:+UseSerialGC`).
- `API_JAVA_OPTS`: extra API JVM options (default none). `JAVA_TOOL_OPTIONS` applies to both JVMs.
- `MCP_STARTUP_TIMEOUT_SECONDS`: how long to wait for the MCP server before failing (default `120`).

Two JVMs run in the container, so give the Droplet at least 1 GB of memory (2 GB is comfortable).

### Optional DigitalOcean deployment

The main-branch Buildkite pipeline has an approval gate before it deploys the published Docker Hub image to a Droplet. The Droplet needs Docker installed, and the SSH user must be allowed to run Docker. Configure these Buildkite secrets:

- `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` (also used by the image-publish step).
- `DO_DROPLET_HOST` (the Droplet IP address or hostname).
- `DO_SSH_PRIVATE_KEY` (private key whose public key is authorized for the SSH user).
- `DO_SSH_KNOWN_HOSTS` (the verified SSH host-key line for the Droplet; pin the fingerprint before adding it).

Optional Buildkite environment variables are `DO_SSH_USER` (default `root`), `DO_DEPLOY_PATH` (default `/opt/parkinglotapi`), `DO_CONTAINER_NAME` (default `parkinglotapi-app`), and `DO_HOST_PORT` (default `80`). Create `${DO_DEPLOY_PATH}/.env` on the Droplet before deployment; the SSH user must be able to read it. The file is kept on the host and is not uploaded by Buildkite. For example:

```dotenv
SPRING_PROFILES_ACTIVE=prod
PROD_DATASOURCE_URL=jdbc:postgresql://DB_HOST:DB_PORT/DB_NAME?sslmode=require
PROD_DATASOURCE_USERNAME=DB_USERNAME
PROD_DATASOURCE_PASSWORD=DB_PASSWORD
SPRING_DATASOURCE_URL=jdbc:postgresql://DB_HOST:DB_PORT/DB_NAME?sslmode=require
SPRING_DATASOURCE_USERNAME=DB_USERNAME
SPRING_DATASOURCE_PASSWORD=DB_PASSWORD
```

The production API and bundled MCP server use different datasource variable names, so set both sets to the same database. Set any other runtime secrets required by the application in that file. Restrict its permissions on the Droplet (`chmod 600`). Allow the configured host port in the Droplet firewall. After approval, Buildkite logs in to Docker Hub on the Droplet, pulls the published image, and recreates the container on host port 80 (mapped to container port 8080).

## Database and Principle of Least Privilege

- Parking lots: no limit
- Floors per parking lot: max `6`
- Sections per floor: max `10`
- Parking spaces per section: max `10`

Validation is enforced in the service layer and at the database layer (Flyway migration `V3__entity_capacity_limits.sql`) to prevent limits from being exceeded.
All previously existing endpoints to change how many of each of these entities exist per parent entity have been removed. I realized end users
don't have any use case for changing those things, it would just be giving users too much access. I removed the endpoints that allowed users to modify
things like floors and sections. If changes are needed the recommended path would be a migration, 3 flyway migrations have already been applied since 
the database's inception so go off those and add a new migration.

The only time those things would need to be changed is if some sort of construction work was done for the airport and the number of parking spaces 
increased or decreased. That would be a rare occurrence and only one person would need to change the database so a migration seems appropriate. 

## Logging

This service uses the New Relic Java agent for APM and log forwarding.

- Set `NEW_RELIC_LICENSE_KEY` in the runtime environment to enable log forwarding.
- Optionally set `NEW_RELIC_APP_NAME` to override the default app name of `parking-lot-api`.
- The Docker image unpacks the checked-in `newrelic/newrelic-java-9.4.0.zip`, copies `newrelic/newrelic.yml`, and starts the JVM with `-javaagent:/app/newrelic/newrelic.jar`.
- For ECS, store the license key in AWS Secrets Manager or SSM Parameter Store and inject it into the task definition as an environment secret.
- Logback continues writing structured JSON to stdout; the New Relic agent handles automatic application log forwarding from the JVM.

## !!! Hard reset the database - Dangerous !!!
### Dev testing only! Staging environments only!

Hard reset flyway database - if manual changes are made to schemas flyway breaks \
Changes to the database should always be done or reverted by adding an additional migration \
``DROP TABLE IF EXISTS flyway_schema_history; `` \
`` DROP SCHEMA public CASCADE; ``\
``CREATE SCHEMA public;``
