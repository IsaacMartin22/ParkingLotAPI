# Parking Lot API Java SDK

The parking application has been retired. The parking-only client and its
exception classes have been removed. This module currently contains no Java
client implementation. Previously published versions do not gain support for
the remaining API endpoints.
 
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

## Build

From the `sdk` folder:

```powershell
mvn clean package
```
