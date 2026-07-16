# API-Service

This is a publicly available API. Currently has no authentication or validation on who can access it.
I may modify some endpoints to add authentication later, but for now it's intended to be completely open to everyone

Uses Maven multi-module build. New version publishing should be triggered only via the Buildkite pipeline on the main branch.
There is the api-service module which is the main application, there is an sdk module which is the sdk for the API service, 
and lastly a shared common module which contains code used in both the api-service and sdk modules.

The common and SDK modules are published to Maven to be added as dependencies in other Java projects. The api service is built 
via a docker image and hosted 24/7 because it powers my portfolio site on my github pages. It is hosted wherever is cheapest 
for me which is currently Render's free tier. The hosted url is currently hardcoded into both the SDK and the frontend query so
if I ever change where it's hosted I will need to update those two places.

API documentation is available at the hosted URL, which should be listed on the github repository page in the upper right corner.
It lists the endpoints available and payload shapes for those endpoints.

## Java SDK

A standalone SDK module is available in `sdk/` with an API client and request/response DTOs for easier integration from other Java projects. See `sdk/README.md` for usage.
If a new SDK is to be published use the Buildkite pipeline, make sure the version is correct in pom.xml, and unblock the sdk release step.

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

## !!! Hard reset the database - Dangerous !!!
### Dev testing only! Staging environments only!

Hard reset flyway database - if manual changes are made to schemas flyway breaks \
Changes to the database should always be done or reverted by adding an additional migration \
``DROP TABLE IF EXISTS flyway_schema_history; `` \
`` DROP SCHEMA public CASCADE; ``\
``CREATE SCHEMA public;``
