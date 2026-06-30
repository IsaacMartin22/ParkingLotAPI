# API-Service

This is a publicly available API. Currently has no authentication or validation on who can access it.
I may modify some endpoints to add authentication later, but for now it's intended to be completely open to everyone

Uses Maven multi-module build.

- Build all modules: `mvn clean package`
- Build API service only: `mvn -pl api-service -am clean package`
- Build SDK only: `mvn -pl sdk -am clean package`

## Java SDK

A standalone SDK module is available in `sdk/` with an API client and request/response DTOs for easier integration from other Java projects. See `sdk/README.md` for usage.
GitHub Packages publishing/consuming steps are also documented in `sdk/README.md`.

### Buildkite optional SDK release

The Buildkite pipeline includes an optional release block at the end on `main`.

- Choose `major`, `minor`, `patch`, or `No release`.
- If a bump is selected, Buildkite will:
  - compute the next semantic version from the latest `sdk-v*` tag,
  - update `sdk/pom.xml` to that exact version,
  - commit the version bump,
  - create and push a tag like `sdk-v1.2.3`.
- Pushing that tag triggers GitHub Actions workflow `.github/workflows/publish-sdk.yml` to publish that SDK version to GitHub Packages.

## Entity Capacity Limits

The following limits are now enforced when creating or re-parenting entities:

- Parking lots: no limit
- Floors per parking lot: max `6`
- Sections per floor: max `10`
- Parking spaces per section: max `10`

Validation is enforced in the service layer and at the database layer (Flyway migration `V3__entity_capacity_limits.sql`) to prevent limits from being exceeded.

Run stage locally with `mvn -pl api-service spring-boot:run "-Dspring-boot.run.profiles=staging"`\
Run stage with `java -jar api-service/target/api-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=stage`\
Run prod with `java -jar api-service/target/api-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`

Connect to the PostgreSQL database configured in `.env`, currently using Beekeeper studio \

Diagnostic data available at ``/actuator/prometheus`` \

Host via ``ngrok http --url=cheesy-elaborate-plating.ngrok-free.dev 8082`` \

- If ngrok address ever changes (Currently on free plan) need to update config.alloy address. See below
- Current Grafana API token shouldn't ever expire, if it does generate a new one on Grafana and update config.alloy

Check Grafana Alloy is running - ``http://localhost:12345/`` \
Grafana Alloy default location - ```%PROGRAMFILES%\GrafanaLabs\Alloy\config.alloy``` \
Grafana monitoring URL endpoint = https://api-service-i1ms.onrender.com/actuator/prometheus

Env Variables for real database found in .env

**Deployment**
The PostgreSQL prod db is hosted on Render, stage db hosted on Aiven
This API Service is also hosted on Render via Docker image + web service, available at https://api-service-i1ms.onrender.com/