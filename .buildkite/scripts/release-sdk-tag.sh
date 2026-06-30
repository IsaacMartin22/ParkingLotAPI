#!/usr/bin/env bash
# release-sdk-tag.sh
# Reads the sdk_release_type build metadata set by the block step, bumps the
# version in sdk/pom.xml accordingly, commits the change, and pushes a Git tag.
#
# Required environment variables:
#   GITHUB_TOKEN  – token with write access to push tags (set as Buildkite secret)

set -euo pipefail

if ! command -v buildkite-agent >/dev/null 2>&1; then
  echo "buildkite-agent is required for release metadata lookup"
  exit 1
fi

RELEASE_TYPE="$(buildkite-agent meta-data get sdk_release_type 2>/dev/null || true)"
if [[ -z "${RELEASE_TYPE}" ]]; then
  RELEASE_TYPE="none"
fi

if [[ "${RELEASE_TYPE}" == "none" ]]; then
  echo "Release type is 'none' – skipping tag creation"
  exit 0
fi

echo "--- :git: Configuring Git identity"
git config user.email "ci@buildkite"
git config user.name  "Buildkite CI"

echo "--- :mag: Reading current SDK version"
CURRENT_VERSION="$(./mvnw -B -ntp -pl sdk help:evaluate -Dexpression=project.version -q -DforceStdout)"
echo "Current version: ${CURRENT_VERSION}"

# Strip -SNAPSHOT suffix if present
BASE_VERSION="${CURRENT_VERSION%-SNAPSHOT}"

IFS='.' read -r MAJOR MINOR PATCH <<< "${BASE_VERSION}"

echo "--- :arrows_counterclockwise: Bumping ${RELEASE_TYPE} version (${MAJOR}.${MINOR}.${PATCH})"
case "${RELEASE_TYPE}" in
  patch) PATCH=$((PATCH + 1)) ;;
  minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
  major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
  *)
    echo "Unknown release type: ${RELEASE_TYPE}"
    exit 1
    ;;
esac

NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}"
echo "New version: ${NEW_VERSION}"

echo "--- :pencil: Updating sdk/pom.xml to ${NEW_VERSION}"
./mvnw -B -ntp -pl sdk versions:set -DnewVersion="${NEW_VERSION}" -DgenerateBackupPoms=false

echo "--- :git: Committing version bump"
git add sdk/pom.xml
git commit -m "chore(sdk): release ${NEW_VERSION} [skip ci]"

echo "--- :label: Creating tag sdk-v${NEW_VERSION}"
git tag "sdk-v${NEW_VERSION}"

echo "--- :arrow_up: Pushing commit and tag"
REPO_URL="https://x-access-token:${GITHUB_TOKEN}@github.com/${BUILDKITE_REPO#*github.com/}"
git remote set-url origin "${REPO_URL}"
git push origin HEAD:"${BUILDKITE_BRANCH}"
git push origin "sdk-v${NEW_VERSION}"

echo "+++ :white_check_mark: Tagged sdk-v${NEW_VERSION} and pushed"

