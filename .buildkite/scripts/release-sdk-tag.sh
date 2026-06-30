#!/usr/bin/env bash
set -euo pipefail

RELEASE_TYPE="none"

if command -v buildkite-agent >/dev/null 2>&1; then
  RELEASE_TYPE="$(buildkite-agent meta-data get sdk_release_type --default none)"
else
  # Fallback to env var for local script testing.
  RELEASE_TYPE="${BUILDKITE_META_sdk_release_type:-none}"
fi

if [[ "${RELEASE_TYPE}" == "none" || -z "${RELEASE_TYPE}" ]]; then
  echo "No SDK release requested. Skipping release step."
  exit 0
fi

if [[ "${RELEASE_TYPE}" != "major" && "${RELEASE_TYPE}" != "minor" && "${RELEASE_TYPE}" != "patch" ]]; then
  echo "Invalid sdk_release_type: ${RELEASE_TYPE}. Expected one of: major, minor, patch, none"
  exit 1
fi

LATEST_TAG="$(git tag --list 'sdk-v*' --sort=-v:refname | head -n 1)"
if [[ -z "${LATEST_TAG}" ]]; then
  CURRENT_VERSION="0.0.0"
else
  CURRENT_VERSION="${LATEST_TAG#sdk-v}"
fi

IFS='.' read -r MAJOR MINOR PATCH <<< "${CURRENT_VERSION}"

MAJOR="${MAJOR:-0}"
MINOR="${MINOR:-0}"
PATCH="${PATCH:-0}"

case "${RELEASE_TYPE}" in
  major)
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
    ;;
  minor)
    MINOR=$((MINOR + 1))
    PATCH=0
    ;;
  patch)
    PATCH=$((PATCH + 1))
    ;;
esac

NEXT_VERSION="${MAJOR}.${MINOR}.${PATCH}"
NEXT_TAG="sdk-v${NEXT_VERSION}"

if git rev-parse -q --verify "refs/tags/${NEXT_TAG}" >/dev/null; then
  echo "Tag ${NEXT_TAG} already exists."
  exit 1
fi

echo "Releasing SDK version ${NEXT_VERSION} using ${RELEASE_TYPE} bump (from ${CURRENT_VERSION})."

# Ensure the SDK pom reflects the exact release version being published.
chmod +x ./mvnw
./mvnw -B -ntp -pl sdk versions:set -DnewVersion="${NEXT_VERSION}" -DgenerateBackupPoms=false

git config user.name "buildkite-bot"
git config user.email "buildkite-bot@users.noreply.github.com"

git add sdk/pom.xml
if ! git diff --cached --quiet; then
  git commit -m "chore(sdk): release ${NEXT_VERSION}"
fi

git tag "${NEXT_TAG}"

git push origin HEAD:main

git push origin "${NEXT_TAG}"

echo "Created and pushed ${NEXT_TAG}."


