# API-Service

Uses maven. Publish jars with `mvn clean package`

Run stage locally with `mvn spring-boot:run "-Dspring-boot.run.profiles=staging"`\
Run stage with `java -jar target/api-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=stage`\
Run prod with `java -jar target/api-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`

Connect to the PostgreSQL database configured in `.env`, currently using Beekeeper studio \

Diagnostic stage data available at ``http://localhost:8082/actuator/prometheus`` \
Diagnostic prod data available at ``http://localhost:8081/actuator/prometheus``

Host stage maybe? via ``ngrok http --url=cheesy-elaborate-plating.ngrok-free.dev 8082`` \
Host prod maybe? via ``ngrok http --url=cheesy-elaborate-plating.ngrok-free.dev 8081``

- If ngrok address ever changes (Currently on free plan) need to update config.alloy address. See below
- Current Grafana API token shouldn't ever expire, if it does generate a new one on Grafana and update config.alloy

Check Grafana Alloy is running - ``http://localhost:12345/`` \
Grafana Alloy default location - ```%PROGRAMFILES%\GrafanaLabs\Alloy\config.alloy``` \
Grafana monitoring URL endpoint = https://cheesy-elaborate-plating.ngrok-free.dev/actuator/prometheus

Env Variables for real database found in .env

**Deployment**
The PostgreSQL prod db is hosted on Render, stage db hosted on Aiven
This API Service is also hosted on Render via Docker image + web service, available at https://api-service-i1ms.onrender.com/