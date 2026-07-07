#!/usr/bin/env bash
# publish-common.sh
# Publishes the parking-lot-common module to Maven Central via the Sonatype Central Portal.
#
# Required environment variables (set as Buildkite secret env vars):
#   MAVEN_CENTRAL_USERNAME  – Central Portal token username
#   MAVEN_CENTRAL_TOKEN     – Central Portal token password
#   GPG_PRIVATE_KEY         – ASCII-armored GPG private key (export with:
#                             gpg --armor --export-secret-keys KEY_ID | base64)
#   GPG_PASSPHRASE          – Passphrase for the GPG key

set -euo pipefail

if ! command -v buildkite-agent >/dev/null 2>&1; then
  echo "buildkite-agent is required for release metadata lookup"
  exit 1
fi

RELEASE_TYPE="$(buildkite-agent meta-data get sdk_release_type 2>/dev/null || true)"
if [[ -z "${RELEASE_TYPE}" || "${RELEASE_TYPE}" == "none" ]]; then
  echo "Release type is 'none' or not set – skipping Maven Central publish"
  exit 0
fi

for required_var in MAVEN_CENTRAL_USERNAME MAVEN_CENTRAL_TOKEN GPG_PRIVATE_KEY GPG_PASSPHRASE; do
  if [[ -z "${!required_var:-}" ]]; then
    echo "${required_var} is required for Maven Central publish"
    exit 1
  fi
done

echo "--- :key: Importing GPG signing key"
echo "${GPG_PRIVATE_KEY}" | base64 --decode | gpg --batch --import
echo "GPG key imported successfully"

echo "--- :globe_with_meridians: Publishing GPG public key to keyservers"
SIGNING_KEY_FINGERPRINT="$(gpg --list-secret-keys --with-colons | awk -F: '/^fpr:/ {print $10; exit}')"
if [[ -z "${SIGNING_KEY_FINGERPRINT}" ]]; then
  echo "Unable to determine GPG key fingerprint from imported secret key"
  exit 1
fi

KEY_UPLOAD_SUCCEEDED=false
for keyserver in hkps://keys.openpgp.org hkps://keyserver.ubuntu.com; do
  if gpg --batch --yes --keyserver "${keyserver}" --send-keys "${SIGNING_KEY_FINGERPRINT}"; then
    echo "Uploaded public key ${SIGNING_KEY_FINGERPRINT} to ${keyserver}"
    KEY_UPLOAD_SUCCEEDED=true
  else
    echo "Warning: failed to upload public key to ${keyserver}"
  fi
done

if [[ "${KEY_UPLOAD_SUCCEEDED}" != "true" ]]; then
  echo "Failed to upload signing key to supported keyservers; Central signature validation may fail"
fi

echo "--- :label: Resolving SDK release version"
SDK_TAG="$(buildkite-agent meta-data get sdk_release_tag 2>/dev/null || true)"
SDK_VERSION="$(buildkite-agent meta-data get sdk_release_version 2>/dev/null || true)"

if [[ -z "${SDK_TAG}" || -z "${SDK_VERSION}" ]]; then
  if command -v git >/dev/null 2>&1; then
    git fetch --tags --force >/dev/null 2>&1 || true
    SDK_TAG="$(git tag -l 'sdk-v*' --sort=-v:refname | head -n1)"
    SDK_VERSION="${SDK_TAG#sdk-v}"
  else
    echo "git is not installed and SDK release metadata was not provided by Buildkite"
    exit 1
  fi
fi

if [[ -z "${SDK_TAG}" ]]; then
  echo "No sdk-v* tag found; refusing to publish a snapshot version to Maven Central"
  exit 1
fi

echo "Publishing common module version ${SDK_VERSION} from tag ${SDK_TAG}"

echo "--- :hammer_and_wrench: Setting common module version"
./mvnw -B -ntp versions:set -DnewVersion="${SDK_VERSION}" -DgenerateBackupPoms=false -DprocessAllModules=true -DprocessParent=true

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
# -Prelease              : activate the release profile (sources, javadoc, GPG, central-publishing if configured)
# -Dgpg.passphrase       : passed directly so the GPG plugin can sign non-interactively
./mvnw -B -ntp \
  -pl parking-lot-common \
  -am \
  -Dgpg.passphrase="${GPG_PASSPHRASE}" \
  deploy

echo "+++ :white_check_mark: parking-lot-common published to Maven Central successfully"


