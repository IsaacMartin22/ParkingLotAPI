# API-Service

This is a publicly available API. Currently has no authentication or validation on who can access it.
I may modify some endpoints to add authentication later, but for now it's intended to be completely open to everyone

Uses maven. Publish jars with `mvn clean package`

Run stage locally with `mvn spring-boot:run "-Dspring-boot.run.profiles=staging"`\
Run stage with `java -jar target/api-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=stage`\
Run prod with `java -jar target/api-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`

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