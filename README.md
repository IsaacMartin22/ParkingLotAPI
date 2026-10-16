# API-Service

This is a publicly available API. Currently has no authentication or validation on who can access it.
I may modify some endpoints to add authentication later, but for now it's intended to be completely open to everyone

Uses Maven multi-module build. New version publishing should be triggered only via the Buildkite pipeline on the main branch.
There is the api-service module which is the main application, there is an sdk module which is the sdk for the API service, 
and a shared common module which contains code used in both the api-service and sdk modules.
The `mcp-server` module is a separate HTTP service exposing tools to the chatbot.

The common and SDK modules are published to Maven to be added as dependencies in other Java projects. The API service is built as a Docker image and hosted 24/7 because it powers my portfolio site on GitHub Pages. Buildkite publishes the image and automatically deploys it to DigitalOcean on `main` after publication succeeds.
The hosted URL is currently hardcoded into both the SDK and the frontend query, so if I ever change where it's hosted I will
need to update those two places.

The repository's container workflow is centered on the root `Dockerfile`, which bundles the API service and the MCP server into one image. DigitalOcean deployment also runs a separate Caddy container for HTTPS. There is no separate Docker Compose setup to maintain.

API documentation is available at the hosted URL, which should be listed on the GitHub repository page in the upper right corner.
It lists the endpoints available and payload shapes for those endpoints.

## Java SDK

A standalone SDK module is available in `sdk/` with an API client and request/response DTOs for easier integration from other Java projects. See `sdk/README.md` for usage.
If a new SDK is to be published use the Buildkite pipeline, make sure the version is correct in pom.xml, and unblock the sdk release step.

## MCP server

The standalone `mcp-server` module uses Java 17 and the MCP Java SDK's HTTP/SSE transport
without changing the existing Spring Boot version. Portfolio search uses MongoDB and an OpenAI key.
Its tools are registered in `mcp-server\src\main\java\parkinglot\mcp\McpServerConfiguration.java`:

- `get_site_analytics` (`SiteAnalyticsTool.java`) reads the API database's `analytics` table and returns
  aggregate statistics only: total events, unique sessions, first/last event times, and optional counts grouped by
  `eventType`, `currentUrl`, `browser`, `operatingSystem`, or `day`. Optional filters are `startTime`, `endTime`,
  `eventType`, and `urlContains`. IP addresses, session ids, and payloads are never returned.
- `search_portfolio_documents` (`PortfolioSearchTool.java`) accepts a `query`, embeds it, and searches
  the portfolio's MongoDB vector index. It preserves the existing candidate count, chunk limit, and literal
  case-insensitive text fallback when vector search returns no matches. Results include text, metadata,
  citations, and relevance scores; embeddings are not returned to the model.

The search tool shares `MONGODB_URI`, `OPENAI_API_KEY`, `OPENAI_EMBEDDING_MODEL` (default
`text-embedding-3-large`), and `PORTFOLIO_CHAT_MAX_CONTEXT_CHUNKS` (default `5`) with the API.
When running the MCP server separately, supply these variables to that process too. The
`app.chat.vector-index` setting now belongs to the MCP server; keep `app.chat.collection` aligned
between the API's seeder and the MCP server if overriding the collection name.

The analytics tool uses the same PostgreSQL database and profile-specific connection variables as the API service.
Both processes inherit `SPRING_PROFILES_ACTIVE`: `prod` uses `PROD_DATASOURCE_URL`,
`PROD_DATASOURCE_USERNAME`, and `PROD_DATASOURCE_PASSWORD`; `staging` uses the corresponding
`STAGING_DATASOURCE_*` variables. Without either profile, both use `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` with the same local defaults.
TLS options are shared through the JDBC URL. Connections are opened read-only
(Hikari `read-only` plus pgjdbc `readOnlyMode=always`) and the tool only issues `SELECT` statements. In deployed
environments, you can additionally configure a separately launched MCP server's active-profile datasource credentials for a role limited
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
Portfolio retrieval is model-selected rather than performed before every answer. The system prompt directs
the model to search for facts about Isaac, while greetings and analytics questions need not search MongoDB.
Citation selection still uses the retrieved documents. Question embeddings remain in the API for interaction
records; the search tool independently embeds its query when called. Retrieval duration now includes the
whole MCP search call (including embedding and transport), summed across search calls; document counts
and citations reflect successful search results. With no search, duration/count are zero and citation is absent.
Tool failures include a safe `errorCode` and generated `errorId` in the API warning and MCP tool response.
Database failures also include `sqlState` (or `unknown` when the driver supplies none).
The API warning translates SQLSTATE into an actionable hint for connection/authentication failures,
missing tables or columns, permissions, and canceled queries. Only the validated SQLSTATE is forwarded,
not raw database messages, connection strings, or tool arguments.
Search the MCP server's console logs for that `errorId` to find the exception and full cause/stack trace
(for database failures), or a validation-rejection warning (`INVALID_ARGUMENTS`).
The OpenAI `callId` belongs to the API and is not the MCP protocol request ID; use `errorId` to correlate
the two processes. Tool arguments and result payloads are not logged; exception traces remain server-side
and should be treated as operationally sensitive.
Chat interactions and tool usage are recorded, but answers are not reused, so tool-backed answers such as
analytics are never stale.
Tool or transport failures follow the chatbot's existing logged error-response path.

### OpenAI model compatibility checks

Buildkite runs unit tests and integration tests in parallel. Classes named `*IntegrationTest`
run only in the integration step, which receives `OPENAI_API_KEY`; all other tests run in
the unit step without that secret. Docker image publishing waits for both steps to succeed.
Modules without matching tests are allowed in each Maven reactor invocation.

`OpenAiChatModel` is the application's supported-model list, not a live OpenAI catalog.
Both the offline request tests and the default-enabled live tests iterate over every enum value,
so adding a model automatically adds coverage. Chat requests use the Responses API and retain
each model's default reasoning settings. Responses are not stored; encrypted reasoning output
is passed back with tool results so reasoning models can continue across tool-calling rounds.

Live OpenAI checks run by default during `test` and require `OPENAI_API_KEY` in the process
environment. To explicitly run offline checks only from the repository root (no credentials
or running services required):

```powershell
.\mvnw.cmd -pl api-service -am "-Dtest=ChatMcpToolsTest,ChatServiceImplTest,OpenAiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dopenai.integration=false" test
```

To run just the live OpenAI checks:

```powershell
.\mvnw.cmd -pl api-service -am "-Dtest=OpenAiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Live checks incur API charges and send only synthetic prompts and tool results to OpenAI.
They check each configured model's availability with `GET /v1/models/{id}`, then exercise
the production Responses API code for a plain answer and a full tool-call/result/answer
round trip. MCP is mocked; PostgreSQL, MongoDB, and the MCP server are not required.
Missing credentials, unavailable models, unsupported request parameters, and tool-flow
failures fail the default test run rather than being silently skipped. Only explicitly setting
`-Dopenai.integration=false` skips the live tests. Run them before deploying model-list changes;
a successful model lookup alone does not establish Responses API or tool compatibility.

### Docker and deployment

The root `Dockerfile` builds a single image that contains both the API service and the MCP server.
`docker-entrypoint.sh` starts the MCP server first on `127.0.0.1:8081`, waits for its port to open, then starts the
API with `CHAT_MCP_BASE_URL=http://127.0.0.1:8081`. Only port 8080 is exposed, so the
MCP server is never reachable from outside the container. If either process exits, the entrypoint stops the other
and the container exits, so ECS (or Docker's restart policy) replaces it. Both processes share the container's
profile-specific datasource settings, so no duplicate database environment variables are required.

Runtime knobs (all optional):

- `MCP_JAVA_OPTS`: MCP server JVM options (default `-XX:MaxRAMPercentage=15 -XX:+UseSerialGC`).
- `API_JAVA_OPTS`: extra API JVM options (default none). `JAVA_TOOL_OPTIONS` applies to both JVMs.
- `MCP_STARTUP_TIMEOUT_SECONDS`: how long to wait for the MCP server before failing (default `120`).

Two JVMs run in the container, so give the Droplet at least 1 GB of memory (2 GB is comfortable).

### Automatic DigitalOcean deployment

The main-branch Buildkite pipeline has an approval gate before it deploys the published Docker Hub image to a Droplet. The Droplet needs Docker installed, and the SSH user must be allowed to run Docker. Configure these Buildkite secrets:

- `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` (also used by the image-publish step).
- `DO_DROPLET_HOST` (the Droplet IP address or hostname).
- `DO_SSH_PRIVATE_KEY` (private key whose public key is authorized for the SSH user).
- `DO_SSH_KNOWN_HOSTS` (the verified SSH host-key line for the Droplet; pin the fingerprint before adding it).

Optional Buildkite environment variables are `DO_SSH_USER` (default `root`), `DO_DEPLOY_PATH` (default `/opt/parkinglotapi`), `DO_CONTAINER_NAME` (default `parkinglotapi-app`), and `DO_API_DOMAIN` (default `api.isaaccmart.in`, hostname only). Set overrides at the pipeline level so deployment and the HTTPS health step use the same domain. `DO_HOST_PORT` is no longer used: Caddy owns public ports 80 and 443, and the API is reachable only on its Docker network. Create `${DO_DEPLOY_PATH}/.env` on the Droplet before deployment; the SSH user must be able to read it. The file is kept on the host and is not uploaded by Buildkite. For example:

```dotenv
SPRING_PROFILES_ACTIVE=prod
PROD_DATASOURCE_URL=jdbc:postgresql://DB_HOST:DB_PORT/DB_NAME?sslmode=require
PROD_DATASOURCE_USERNAME=DB_USERNAME
PROD_DATASOURCE_PASSWORD=DB_PASSWORD
```

The production API and bundled MCP server both use the `PROD_DATASOURCE_*` variables above; a duplicate
`SPRING_DATASOURCE_*` set is not needed. Set any other runtime secrets required by the application in that file.
Restrict its permissions on the Droplet (`chmod 600`).

Before the first HTTPS deployment:

- In GoDaddy, point the `api` A record at the Droplet's public IPv4 address (currently `159.203.113.71`). Leave the frontend's DNS records unchanged. Remove any stale AAAA record unless IPv6 also routes to this Droplet.
- Allow inbound TCP ports **80 and 443** in both the DigitalOcean cloud firewall and the host firewall. Caddy needs port 80 for certificate issuance/renewal and HTTP-to-HTTPS redirects. Ensure no other host service occupies these ports.
- Merge these deployment changes into `main` and run Buildkite; DigitalOcean deployment starts automatically after Docker image publication succeeds.

Buildkite pulls the API image, reuses the locally cached `caddy:2-alpine` image without pulling or checking for updates, creates a shared Docker network, and recreates the API without a published host port. The Caddy image must already be present on the Droplet; deployment fails if it is missing. A separate `<DO_CONTAINER_NAME>-caddy` container proxies requests to the API on port 8080 and automatically obtains and renews a publicly trusted certificate. Certificate state persists in named Docker volumes across deployments; do not delete the Caddy data volume during routine cleanup. Spring uses Caddy's forwarded headers to recognize the original HTTPS request, and streaming responses are not buffered.

The deployment step has a ten-minute Buildkite timeout and logs the API image pull. If it stalls after `Using default tag: latest`, the Droplet has reached the API image pull; this message only means the image reference omitted a tag, not that application startup failed. On the Droplet, run `docker pull isaaccmartin151/parkinglotapi-app:latest` as the deployment user and inspect `journalctl -u docker --since "10 minutes ago"` for registry connection, DNS, or daemon errors. Docker Hub credentials used by the publish step are not forwarded to the Droplet; private images require a separate `docker login` there. A Buildkite timeout does not guarantee an already-running remote Docker operation has stopped; inspect the Droplet before retrying.

Before replacing containers, deployment checks Docker containers publishing ports 80 or 443. Older API containers using the same image repository (including older tags) are stopped automatically, even if their names differ. The confirmed legacy container named `parkinglot-api` using the local image repository `parkinglot-api` is also allowed to stop; both must match. Legacy containers are stopped, not removed. The configured API and Caddy containers are removed and recreated as usual. Unrelated containers are not stopped: deployment fails with their name and image so port conflicts can be resolved deliberately. Host services such as Nginx must still be handled separately.

Deployment waits for `http://api:8080/api/diagnostics/ping` from the proxy's Docker network before replacing Caddy. If the API never becomes ready, deployment fails and prints the API container state and recent API/Caddy logs. The public health step requests `https://api.isaaccmart.in/api/diagnostics/ping` (or the `DO_API_DOMAIN` override) with normal certificate verification and retries while Caddy obtains its certificate. `/api` is not a health endpoint. If the public check fails after internal readiness succeeds, inspect Caddy's logs and the domain's DNS records. The API image itself still serves HTTP internally; no Dockerfile or Java keystore changes are needed.

Update the frontend's API base URL to `https://api.isaaccmart.in` and redeploy the frontend. All browser API requests, including SSE connections, must use HTTPS explicitly; an HTTP-to-HTTPS redirect cannot rescue a request the browser blocks as mixed content.

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
