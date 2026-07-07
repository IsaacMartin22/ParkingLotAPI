# Parking Lot API Java SDK

This module provides a lightweight Java client for the Parking Lot API so other Java projects can call endpoints without manually building HTTP requests.

## What is included

- `ParkingLotApiClient` with endpoint methods for cars, spaces, floors, sections, lots, floor details, and diagnostics.
- Request DTOs for write operations:
  - `ParkingSpaceUpdateRequest`
- Response DTOs matching API payloads.
- A tiny runner example in `com.example.parkinglot.sdk.example.SdkQuickstart`.
 
## Coordinates

`groupId`: `io.github.isaacmartin22`

`artifactId`: `parking-lot-api-sdk`

`version`: `0.0.1-SNAPSHOT` (development)

## GitHub Packages (no local install required)

This repo is configured with a publish workflow at `.github/workflows/publish-sdk.yml`.

### Publish the SDK package

Use one of these options:

1. Run the workflow manually from GitHub Actions: `Publish SDK to GitHub Packages` and enter a version (example: `0.1.0`).
2. Push a tag matching `sdk-v*` (example: `sdk-v0.1.0`). The `sdk-v` prefix is stripped and `0.1.0` is published.

The workflow deploys the `sdk` module to:

`https://maven.pkg.github.com/<OWNER>/<REPO>`

where `<OWNER>/<REPO>` is the repository that ran the workflow.

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
        <version>0.1.0</version>
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

Use a GitHub token that can read packages from the repository.

- Private repos: token needs `read:packages` and repo access.
- Public repos: token still needed for Maven authentication in most setups.

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
  - `getDiagnostics()`

## Error handling

- Non-2xx responses throw `ApiClientHttpException`.
- Network/serialization issues throw `ApiClientException`.

## Build

From the `sdk` folder:

```powershell
mvn clean package
```

