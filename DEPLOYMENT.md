# API Service - Deployment & Network Access Guide

## Running the Application Independently

### Option 1: Run from Source with Maven
```powershell
mvn clean install
mvn spring-boot:run
```

The application will start on port 8080 and bind to all network interfaces (0.0.0.0).

### Option 2: Build a Standalone JAR and Run

Build the JAR:
```powershell
mvn clean package
```

Run the JAR:
```powershell
java -jar target/api-service-0.0.1-SNAPSHOT.jar
```

## Accessing from Other Machines

### Finding Your Machine's IP Address

**On Windows (PowerShell):**
```powershell
ipconfig
```
Look for "IPv4 Address" for your local network IP (typically 192.168.x.x or 10.x.x.x).

**From Another Machine:**
- Get the IP address of the machine running the API service
- Use that IP instead of `localhost`

### Example URLs

Replace `YOUR_IP` with your actual IP address:

- **Base URL:** `http://YOUR_IP:8080`
- **List all cars:** `http://YOUR_IP:8080/api/cars`
- **Get car by ID:** `http://YOUR_IP:8080/api/cars/1`
- **List all parking spaces:** `http://YOUR_IP:8080/api/spaces`
- **List all parking lots:** `http://YOUR_IP:8080/api/lots`
- **H2 Console:** `http://YOUR_IP:8080/h2-console`

### Example POST Request

From another machine (replace `YOUR_IP`):

```powershell
$body = @{
    make = "Tesla"
    model = "Model 3"
    year = 2024
    licensePlate = "TESLA-01"
    parked = $false
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://YOUR_IP:8080/api/cars" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body
```

Or with curl:
```bash
curl -X POST http://YOUR_IP:8080/api/cars \
  -H "Content-Type: application/json" \
  -d '{"make":"Tesla","model":"Model 3","year":2024,"licensePlate":"TESLA-01","parked":false}'
```

## Configuration

The application reads settings from `src/main/resources/application.properties`:

- `server.address=0.0.0.0` - Listens on all network interfaces
- `server.port=8080` - Port number
- `spring.h2.console.enabled=true` - Enables the H2 admin console
- `spring.jpa.hibernate.ddl-auto=create-drop` - Auto-creates/drops tables on restart

To change the port, modify `application.properties`:
```properties
server.port=9090
```

## Firewall Considerations

If you can't access the application from another machine:

1. **Windows Firewall:** 
   - Add port 8080 to Windows Firewall exceptions
   - Go to Windows Defender Firewall → Allow an app through firewall
   - Add Java or maven to the allowed list

2. **Router/Network Firewall:**
   - Ensure port 8080 is not blocked by your network firewall
   - If behind a corporate firewall, consult your IT department

## H2 Database Console

Access the database browser:
- URL: `http://YOUR_IP:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave empty)

**Note:** The in-memory database resets every time the app restarts. For persistent storage, update `application.properties`:
```properties
spring.datasource.url=jdbc:h2:file:./data/testdb
```

## Production Deployment

For production, consider:

1. **Using a persistent database** (PostgreSQL, MySQL, etc.)
2. **Running behind a reverse proxy** (Nginx, Apache)
3. **Using Docker** for containerization
4. **Adding HTTPS/SSL** configuration
5. **Environment-specific properties** (dev, staging, production)
6. **Logging and monitoring** setup

## Troubleshooting

### Table Not Found Error
- Ensure `spring.jpa.hibernate.ddl-auto=create-drop` is set in `application.properties`
- This auto-creates tables when the app starts

### Port Already in Use
- Change the port in `application.properties`
- Or kill the process using port 8080: `netstat -ano | findstr :8080`

### Can't Connect from Another Machine
- Verify your IP address with `ipconfig`
- Check Windows Firewall settings
- Ensure the API service machine and client are on the same network
- Test locally first: `http://localhost:8080/api/cars`

