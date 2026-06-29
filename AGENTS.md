# AGENTS.md

## Agent Execution Policy
- Do **not** run terminal commands by default.
- Treat command execution as opt-in: only run commands when the user explicitly asks for it in the current request.
- Prefer static analysis (reading/editing files) over executing builds, tests, scripts, package managers, Docker, or Git commands.
- If command execution seems necessary, ask the user first and wait for confirmation.
- Do not start background processes (servers/watchers) unless explicitly requested.
- Do not run auto-fix or formatting commands automatically; propose command(s) for the user to run instead.

## Project Snapshot
- Spring Boot 3.1 + Java 17 Maven API service for parking-lot inventory and occupancy events (`pom.xml`, `src/main/java/com/example/apiservice`).
- Persistence is PostgreSQL via JPA; schema and seed data are managed by Flyway (`src/main/resources/db/migration`).
- Static API docs/reference page is served from `src/main/resources/static/index.html`.

## Architecture That Matters
- Main flow is `Controller -> Service -> Repository -> JPA Entity`; many endpoints map entities to response DTOs via static mapper classes.
- Domain hierarchy is `ParkingLot -> Floor -> Section -> ParkingSpace -> Car` (`src/main/java/com/example/apiservice/dbentity`).
- Deep floor-details reads use eager loading through `@EntityGraph` in `FloorRepository.findWithSectionsAndParkingSpacesById`.
- Real-time updates are SSE streams keyed by `{lotId}:{floorId}` (`FloorEventService`, `FloorEventsController`).

## Domain and API Conventions
- Operationally common writes are `Car` and `ParkingSpace`; `Floor` and `Section` mutation endpoints exist but are called out as rare in `index.html`.
- `ParkingLot.type` is enum-backed and stored as string (`ParkingLotType`: `Economy`, `LongTerm`, `ShortTerm`, `Waiting`).
- Relationship references in request payloads use nested IDs (example: `"parkingSpace": { "id": 10 }`) as shown in `index.html` samples.
- Mapper pattern is hand-written static utilities under `src/main/java/com/example/apiservice/mapper` (no MapStruct layer).

## Observability and Runtime Signals
- Every request is intercepted by `DiagnosticsInterceptor`, timed, labeled by route pattern, and recorded through `MetricsService`.
- Prometheus scraping endpoint is `/actuator/prometheus` (enabled in `application.properties`).
- Custom runtime diagnostics endpoint is `GET /api/diagnostics` (`DiagnosticsController`, `DiagnosticsService`).

## Config and Environment Profiles
- Base config (`application.properties`) uses local Postgres defaults with `spring.jpa.hibernate.ddl-auto=validate`.
- Profile files present are `application-staging.properties` and `application-prod.properties`; profile keys are `staging` and `prod`.
- `README.md` mentions `stage` once; for Spring profile activation, use `staging` to match the actual file.

## Build, Test, and Run Workflows
- Build JAR: `mvn clean package`.
- Run default profile: `mvn spring-boot:run`.
- Run staging profile: `mvn spring-boot:run "-Dspring-boot.run.profiles=staging"`.
- Run tests: `mvn test`.
- Current automated tests are mapper-focused (`src/test/java/com/example/apiservice/mapper`).
- Docker build is multi-stage Temurin 17 and currently skips tests during image packaging (`Dockerfile`).

## Change Safety Checklist for Agents
- If you modify nested floor-details response structure, update both `FloorDetailsMapper` and `FloorDetailsMapperTest`.
- If you change `CarService.save` or `ParkingSpaceService.save`, preserve SSE publication semantics for floor-scoped updates.
- If you add API endpoints, they will be auto-instrumented by interceptor routing; keep route patterns stable for metric cardinality.
- For schema changes, create a new Flyway migration (`V{N}__description.sql`); do not rewrite existing applied migration files.
