# Parking Lot API Java SDK

This module provides a lightweight Java client for the Parking Lot API so other Java projects can call endpoints without manually building HTTP requests.

## What is included

- `ParkingLotApiClient` with endpoint methods for spaces, floors, sections, lots, floor details, and diagnostics.
- Request DTOs for write operations:
  - `ParkingSpaceUpdateRequest`
- Response DTOs matching API payloads.
 
## Coordinates

`groupId`: `io.github.isaacmartin22`

`artifactId`: `parking-lot-api-sdk`

`version`: `0.0.8` (As of 7/7/2026 - see https://central.sonatype.com/artifact/io.github.isaacmartin22/parking-lot-api-sdk for latest version)

### Publish the SDK package

Unblock the "Publish SDK" step in the buildkite pipeline in the repository

### Consume from another Maven project

In the consuming project's `pom.xml`, add the GitHub Packages repository and dependency.

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/<OWNER>/<REPO></url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.github.isaacmartin22</groupId>
        <artifactId>parking-lot-api-sdk</artifactId>
        <version>0.0.8</version>
    </dependency>
</dependencies>
```

Then add credentials in `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>GITHUB_USERNAME</username>
            <password>GITHUB_TOKEN</password>
        </server>
    </servers>
</settings>
```

## Quick usage

```java
import com.example.parkinglot.sdk.ParkingLotApiClient;

ParkingLotApiClient client = new ParkingLotApiClient("https://api-service-i1ms.onrender.com");

## API client methods

- Parking spaces
  - `getParkingSpaces()`
  - `getParkingSpace(long id)`
  - `updateParkingSpace(long id, ParkingSpaceUpdateRequest request)`
  - `removeCar(long id)`
- Floors
  - `getFloors()`
  - `getFloor(long id)`
- Sections
  - `getSections()`
  - `getSection(long id)`
- Lots
  - `getParkingLots()`
  - `getParkingLot(long id)`
  - `getFloorDetailsForLot(long lotId, long floorId)`
- Diagnostics
  - `getAPIDiagnostics()`
  - `getDatabaseDiagnostics()`

## Error handling

- Non-2xx responses throw `ApiClientHttpException`.
- Network/serialization issues throw `ApiClientException`.

## Build

From the `sdk` folder:

```powershell
mvn clean package
```

