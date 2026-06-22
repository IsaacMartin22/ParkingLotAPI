# API-Service

Run with `mvn spring-boot:run`

Uses maven. Publish jars with `mvn clean package`

Run stage locally with `mvn spring-boot:run "-Dspring-boot.run.profiles=staging"`\
Run stage with `java -jar target/api-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=stage`\
Run prod with `java -jar target/api-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`

Check local stage database via ``http://localhost:8082/h2-console`` \
Check local prod database via ``http://localhost:8081/h2-console`` 

Diagnostic stage data available at ``http://localhost:8082/actuator/prometheus`` \
Diagnostic prod data available at ``http://localhost:8081/actuator/prometheus``
 
Host stage maybe? via ``ngrok http --url=cheesy-elaborate-plating.ngrok-free.dev 8082`` \
Host prod maybe? via ``ngrok http --url=cheesy-elaborate-plating.ngrok-free.dev 8081``

- If ngrok address ever changes (Currently on free plan) need to update config.alloy address. See below
- Current Grafana API token shouldn't ever expire, if it does generate a new one on Grafana and update config.alloy

Check Grafana Alloy is running - ``http://localhost:12345/`` \
Grafana Alloy default location - ```%PROGRAMFILES%\GrafanaLabs\Alloy\config.alloy``` \
Grafana monitoring URL endpoint = https://cheesy-elaborate-plating.ngrok-free.dev/actuator/prometheus

Env Variables for real database - Need to add JDBC driver dependency and set Hibernate settings

### **Stage**

``SPRING_PROFILES_ACTIVE=staging
STAGING_DATASOURCE_URL=jdbc:postgresql://staging-db:5432/api_service
STAGING_DATASOURCE_USERNAME=api_service_staging
STAGING_DATASOURCE_PASSWORD=staging-password
SERVER_PORT=8082``

### **Prod**
``SPRING_PROFILES_ACTIVE=prod
PROD_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/api_service
PROD_DATASOURCE_USERNAME=api_service_prod
PROD_DATASOURCE_PASSWORD=prod-password
SERVER_PORT=8081``