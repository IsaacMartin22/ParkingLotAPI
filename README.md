# API-Service

Run with `mvn spring-boot:run`

Uses maven. Publish jars with `mvn clean package` and run with `java -jar target/JAR_NAME.jar`

Check local database via ``http://localhost:8081/h2-console``

Diagnostic data available at ``http://localhost:8081/actuator/prometheus``

Host via ``ngrok http --url=cheesy-elaborate-plating.ngrok-free.dev 8081``
- If ngrok address ever changes (Currently on free plan) need to update config.alloy address. See below
- Current Grafana API token shouldn't ever expire, if it does generate a new one on Grafana and update config.alloy

Check Grafana Alloy is running - ``http://localhost:12345/``
Grafana Alloy default location - ```%PROGRAMFILES%\GrafanaLabs\Alloy\config.alloy```
Grafana monitoring URL endpoint = https://cheesy-elaborate-plating.ngrok-free.dev/actuator/prometheus

