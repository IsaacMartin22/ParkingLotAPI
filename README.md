# API-Service

This is a publicly available API. Currently has no authentication or validation on who can access it.
I may modify some endpoints to add authentication later, but for now it's intended to be completely open to everyone

Uses Maven multi-module build. New version publishing should be triggered only via the Buildkite pipeline on the main branch.
There is the api-service module which is the main application, there is an sdk module which is the sdk for the API service, 
and a shared common module which contains code used in both the api-service and sdk modules.
The `mcp-server` module is a separate HTTP service exposing tools to the chatbot.

The common and SDK modules are published to Maven to be added as dependencies in other Java projects. The API service is built as a Docker image and hosted 24/7 because it powers my portfolio site on GitHub Pages. Buildkite publishes the image and automatically deploys it to DigitalOcean on `main` after publication succeeds.
The hosted URL is configured in the frontend query, so hosting changes require updating that URL.

The parking application has been retired. Flyway migration V11 permanently drops
`parking_spaces`, `sections`, `floors`, and `parking_lots`, including their data,
and removes the capacity-limit functions. Earlier migrations remain unchanged.
The parking entities, shared DTOs and events, and parking-only SDK client have
been removed. Analytics, chatbot, and other portfolio features remain available.

The repository's container workflow is centered on the root `Dockerfile`, which bundles the API service and the MCP server into one image. DigitalOcean deployment also runs a separate Caddy container for HTTPS. There is no separate Docker Compose setup to maintain.

API documentation is available at the hosted URL, which should be listed on the GitHub repository page in the upper right corner.
It lists the endpoints available and payload shapes for those endpoints.

## TikTok video downloads

`GET /api/gubb?url=<URL-encoded HTTPS TikTok URL>` queues a background download and
returns HTTP 202 immediately, rather than keeping the request open during processing:

```json
{
  "jobId": "<id>",
  "status": "queued",
  "statusUrl": "https://<API host>/api/gubb/jobs/<id>",
  "url": null,
  "error": null
}
```

Poll `statusUrl` every two seconds. Both endpoints use `Cache-Control: no-store`.
The initial response also sets `Location` to `statusUrl`.
`GET /api/gubb/jobs/<id>` returns HTTP 202 with `Retry-After: 2` while the status is
`queued` or `processing`. It returns HTTP 200 with `status: "completed"` and
`url: "https://<API host>/api/gubb/videos/<id>"` once the video is ready.
A failed job returns HTTP 200 with `status: "failed"` and an `error` message.
Cancelled jobs return HTTP 200 with `status: "cancelled"` and an `error` message.
Stop polling on any terminal status. Never download a video unless the status is
`completed` and `url` is present. Unknown or expired jobs return HTTP 404.
`DELETE /api/gubb/jobs/<id>` cancels a queued or running job and returns its current
status. Repeated cancellation is safe; completed and failed jobs remain unchanged.
Cancellation interrupts the worker, kills its active subprocess and child processes,
and removes partial files without starting the next stage.

On an initial request or polling failure, stop the client flow and cancel the known
job with `DELETE`. Use a fresh request signal for cancellation, not an already-aborted
signal. If the initial response is lost or cancellation cannot reach the API, the
server cancels pending jobs after 30 seconds without a successful status lookup.
The lease starts when the job is created, including time spent queued, and each
status lookup renews it. An expired lease cannot be revived by polling again.
A lightweight sweep checks leases every second; stage boundaries also check them.
The server cannot detect a lost polling response immediately.
Existing clients must switch from reading `url` immediately to polling `statusUrl`.
For example, frontend code can use:

```javascript
async function getTikTokVideoUrl(apiBase, videoUrl, signal) {
  let statusUrl;
  try {
    let response = await fetch(
      `${apiBase}/api/gubb?url=${encodeURIComponent(videoUrl)}`,
      { cache: "no-store", signal }
    );
    while (true) {
      if (!response.ok) {
        throw new Error(`Video request failed (${response.status})`);
      }
      const job = await response.json();
      if (typeof job.statusUrl === "string") statusUrl = job.statusUrl;
      signal?.throwIfAborted();
      if (job.status === "failed" || job.status === "cancelled") {
        throw new Error(job.error || `Video job ${job.status}`);
      }
      if (job.status === "completed") {
        if (!job.url) throw new Error("Completed job has no video URL");
        return job.url;
      }
      if (!["queued", "processing"].includes(job.status) || !job.statusUrl) {
        throw new Error("Invalid video job response");
      }
      await new Promise(resolve => setTimeout(resolve, 2000));
      signal?.throwIfAborted();
      response = await fetch(statusUrl, { cache: "no-store", signal });
    }
  } catch (error) {
    if (statusUrl) {
      try {
        const cancellation = await fetch(statusUrl, {
          method: "DELETE",
          keepalive: true,
          signal: AbortSignal.timeout(5000)
        });
        if (!cancellation.ok) {
          throw new Error(`Cancellation failed (${cancellation.status})`);
        }
      } catch (cancellationError) {
        console.warn("Cancellation could not be confirmed; the job lease will expire.", cancellationError);
      }
    }
    throw error;
  }
}
```

The returned video URL streams the stored MP4, including HTTP Range support for
browser playback and seeking. Download and transcode each have a five-minute timeout.
One job runs at a time on a dedicated minimum-priority worker; up to twenty more
wait in the bounded queue. A full queue returns HTTP 503 instead of running work on
an HTTP request thread. Downloads run with a 2 MiB/s
rate limit. Both subprocesses use Linux `nice -n 19` and idle I/O scheduling
(`ionice -c 3`) so normal API work takes precedence. FFmpeg uses one decoder thread,
one encoder thread, and single-threaded filters. yt-dlp does not perform a separate
hidden transcode. These limits reduce contention, not provide hard CPU or memory isolation.
Job status is held in memory for seven days after completion, failure, or cancellation;
restarting the API loses job status, but already published videos remain available.
Logs include job IDs, download/transcode stages, elapsed processing time, and failures.
Videos are publicly accessible to anyone with the URL, with no authentication.

Videos expire seven days after download and return 404 thereafter. An hourly task deletes
expired files (also on startup), so disk cleanup may lag expiration by up to an hour.
Set `GUBB_STORAGE_DIRECTORY` to a writable directory (default `./data/gubb`).
For Docker, mount persistent storage at that path; the DigitalOcean deploy script automatically
uses the named volume `<container-name>-gubb-videos` at `/app/data/gubb`, preserving videos
across container replacements on the same host. Other deployments must mount their own storage.
The runtime needs `yt-dlp`, Python 3, FFmpeg, `nice`, and `ionice`, already installed
by the root `Dockerfile`.

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
- `create_trello_card` (`TrelloCreateCardTool.java`) creates a card in an open column on one configured
  Trello board. Required arguments are `listId` (the column's full 24-character hexadecimal ID) and
  `name`; `description` is optional. Names must be non-blank; names and descriptions are limited to
  16,384 characters. The tool verifies the column's board before creating a card and returns
  the card's `id`, `url`, `name`, `listId`, and `boardId`.

To enable Trello writes, supply `TRELLO_API_KEY`, `TRELLO_API_TOKEN` (a Trello user token with read/write
access to the board), and `TRELLO_BOARD_ID` (the full 24-character board ID, not its short URL ID).
An API key alone cannot authorize card creation. Keep both credentials in the environment, not tool
arguments or source control. The tool remains discoverable without configuration but returns
`CONFIGURATION_ERROR` without contacting Trello. Any open column on the configured board is allowed;
other boards and archived columns are rejected.

Example tool arguments:

```json
{
  "listId": "0123456789abcdef01234567",
  "name": "Follow up on parking lot feedback",
  "description": "Review the feedback and record next steps."
}
```

Set these variables on the MCP process, or in the Droplet's runtime `.env` file for the bundled Docker
deployment. Obtain board and list IDs from Trello's board JSON export (`id` and each entry in `lists`).
After a creation error, check the board before retrying: a lost response may still mean a card was
created. The tool does not retry writes automatically.

**Write-access warning:** the API automatically exposes discovered MCP tools to its chatbot.
Configuring Trello therefore lets chatbot callers request card creation on this board. Use a dedicated
board and a least-privilege token; add caller authorization before enabling writes for untrusted users.

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
Add authentication before exposing it publicly or enabling sensitive tools such as Trello writes.

The API's MCP integration is always on. The API connects to `CHAT_MCP_BASE_URL` (default `http://localhost:8081`)
at startup and fails startup if the MCP server is unavailable, so when running locally start the MCP server first.
`ChatServiceImpl` discovers the tools and lets the model select them using `tool_choice=auto`,
then sends their results back for the final answer, with at most four tool-calling rounds.

Frontends can discover the same tools with `GET /api/chat/tools`, alongside
`GET /api/chat/models`. The response contains a `tools` array; each tool includes
its `name`, optional `description`, and JSON Schema `inputSchema`, for example:

```json
{
  "tools": [
    {
      "name": "search_portfolio_documents",
      "description": "Search portfolio documents",
      "inputSchema": {
        "type": "object",
        "properties": { "query": { "type": "string" } },
        "required": ["query"]
      }
    }
  ]
}
```

The endpoint queries the connected MCP server on each request and does not invoke
tools or call OpenAI. No advertised tools produces `{"tools":[]}`; discovery
failures produce an HTTP error, not an empty list. Discovery indicates which tools
the chatbot can select, not that every tool's dependencies or credentials are healthy.

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
