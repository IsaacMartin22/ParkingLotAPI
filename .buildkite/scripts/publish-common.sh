#!/usr/bin/env bash
# publish-common.sh
# Publishes the parking-lot-common module to Maven Central via the Sonatype Central Portal.
#
# Required environment variables (set as Buildkite secret env vars):
#   MAVEN_CENTRAL_USERNAME  – Central Portal token username
#   MAVEN_CENTRAL_TOKEN     – Central Portal token password
#   COMMON_RELEASE_VERSION  – Optional override for the version to publish

set -euo pipefail

for required_var in MAVEN_CENTRAL_USERNAME MAVEN_CENTRAL_TOKEN; do
  if [[ -z "${!required_var:-}" ]]; then
    echo "${required_var} is required for Maven Central publish"
    exit 1
  fi
done

COMMON_VERSION="${COMMON_RELEASE_VERSION:-$(./mvnw -B -ntp -pl parking-lot-common help:evaluate -Dexpression=project.version -q -DforceStdout)}"

if [[ -z "${COMMON_VERSION}" ]]; then
  echo "Unable to determine parking-lot-common version"
  exit 1
fi

if [[ "${COMMON_VERSION}" == *"SNAPSHOT"* ]]; then
  echo "Refusing to publish snapshot version ${COMMON_VERSION} to Maven Central"
  exit 1
fi

echo "Publishing common module version ${COMMON_VERSION}"

echo "--- :wrench: Writing Maven settings.xml"
SETTINGS_FILE="${HOME}/.m2/settings.xml"
mkdir -p "$(dirname "${SETTINGS_FILE}")"

cat > "${SETTINGS_FILE}" <<EOF
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <!-- Must match <publishingServerId>central</publishingServerId> in parking-lot-common/pom.xml -->
      <id>central</id>
      <username>${MAVEN_CENTRAL_USERNAME}</username>
      <password>${MAVEN_CENTRAL_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF

echo "--- :package: Building and publishing parking-lot-common to Maven Central"
# -pl parking-lot-common : build only the common module
# -am                     : also build modules it depends on (none currently, but safe)
# -Prelease              : activate the release profile (sources, javadoc, central-publishing)
./mvnw -B -ntp \
  -pl parking-lot-common \
  -am \
  -Prelease \
  -DskipTests \
  deploy

echo "+++ :white_check_mark: parking-lot-common published to Maven Central successfully"


