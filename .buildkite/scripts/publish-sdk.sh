#!/usr/bin/env bash
# publish-sdk.sh
# Publishes the SDK module to Maven Central via the Sonatype Central Portal.
#
# Required environment variables (set as Buildkite secret env vars):
#   MAVEN_CENTRAL_USERNAME  – Central Portal token username
#   MAVEN_CENTRAL_TOKEN     – Central Portal token password
#   GPG_PRIVATE_KEY         – ASCII-armored GPG private key (export with:
#                             gpg --armor --export-secret-keys KEY_ID | base64)
#   GPG_PASSPHRASE          – Passphrase for the GPG key

set -euo pipefail

echo "--- :key: Importing GPG signing key"
echo "${GPG_PRIVATE_KEY}" | base64 --decode | gpg --batch --import
echo "GPG key imported successfully"

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
      <!-- Must match <publishingServerId>central</publishingServerId> in sdk/pom.xml -->
      <id>central</id>
      <username>${MAVEN_CENTRAL_USERNAME}</username>
      <password>${MAVEN_CENTRAL_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF

echo "--- :package: Building and publishing SDK to Maven Central"
# -pl sdk        : build only the SDK module
# -am            : also build modules sdk depends on (none currently, but safe)
# -Prelease      : activate the release profile (sources, javadoc, GPG, central-publishing)
# -Dgpg.passphrase: passed directly so the GPG plugin can sign non-interactively
./mvnw -B -ntp \
  -pl sdk \
  -Prelease \
  -Dgpg.passphrase="${GPG_PASSPHRASE}" \
  deploy

echo "+++ :white_check_mark: SDK published to Maven Central successfully"

