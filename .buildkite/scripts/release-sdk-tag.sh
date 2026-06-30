#!/usr/bin/env bash
# release-sdk-tag.sh
# Reads sdk_release_type build metadata, calculates next semantic version, and
# creates/pushes a Git tag used by the publish workflow.
#
# Required environment variables:
#   GITHUB_TOKEN  – token with write access to push tags (set as Buildkite secret)

set -euo pipefail

if ! command -v git >/dev/null 2>&1; then
  echo "--- :package: git not found, installing"
  if command -v apt-get >/dev/null 2>&1; then
    apt-get update
    apt-get install -y git openssh-client
  elif command -v apk >/dev/null 2>&1; then
    apk add --no-cache git openssh-client
  elif command -v yum >/dev/null 2>&1; then
    yum install -y git openssh-clients
  else
    echo "git is required but no supported package manager was found"
    exit 1
  fi
fi

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

if [[ -z "${GITHUB_TOKEN:-}" ]]; then
  echo "GITHUB_TOKEN is required for non-interactive tag push in CI"
  exit 1
fi

# Never allow interactive credential prompts in CI; fail fast instead.
export GIT_TERMINAL_PROMPT=0

ORIGIN_URL="$(git remote get-url origin)"
SOURCE_REPO_URL="${BUILDKITE_REPO:-${ORIGIN_URL}}"
REPO_PATH="${SOURCE_REPO_URL#*github.com[:/]}"
REPO_PATH="${REPO_PATH#git@github.com:}"
REPO_PATH="${REPO_PATH#ssh://git@github.com/}"
REPO_PATH="${REPO_PATH#https://github.com/}"
REPO_PATH="${REPO_PATH%.git}"

PUSH_URL="origin"
if [[ -n "${GITHUB_TOKEN:-}" && -n "${REPO_PATH}" && "${REPO_PATH}" != "${SOURCE_REPO_URL}" ]]; then
  PUSH_URL="https://x-access-token:${GITHUB_TOKEN}@github.com/${REPO_PATH}.git"
  echo "--- :lock: Using token-authenticated push URL"
elif [[ "${ORIGIN_URL}" == git@github.com:* || "${ORIGIN_URL}" == ssh://git@github.com/* ]]; then
  PUSH_URL="origin"
  echo "--- :key: Using existing SSH origin for push"
else
  echo "No authenticated GitHub push method available."
  echo "Set GITHUB_TOKEN for HTTPS push, or configure origin as SSH with a valid deploy key on the agent."
  exit 1
fi

if [[ "${PUSH_URL}" == git@github.com:* || "${PUSH_URL}" == ssh://git@github.com/* || "${PUSH_URL}" == "origin" && ( "${ORIGIN_URL}" == git@github.com:* || "${ORIGIN_URL}" == ssh://git@github.com/* ) ]]; then
  echo "--- :shield: Preparing known_hosts for github.com"
  mkdir -p "${HOME}/.ssh"
  chmod 700 "${HOME}/.ssh"
  KNOWN_HOSTS_FILE="${HOME}/.ssh/known_hosts"
  touch "${KNOWN_HOSTS_FILE}"
  chmod 600 "${KNOWN_HOSTS_FILE}"
  if ! ssh-keygen -F github.com -f "${KNOWN_HOSTS_FILE}" >/dev/null 2>&1; then
    ssh-keyscan -H github.com >> "${KNOWN_HOSTS_FILE}" 2>/dev/null
  fi
fi

echo "--- :git: Configuring Git identity"
git config user.email "ci@buildkite"
git config user.name  "Buildkite CI"

echo "--- :mag: Reading current SDK version"
CURRENT_VERSION="$(./mvnw -B -ntp -pl sdk help:evaluate -Dexpression=project.version -q -DforceStdout)"
echo "Current version: ${CURRENT_VERSION}"

# Prefer latest release tag as source of truth for next bump; fallback to pom base version.
git fetch --tags --force >/dev/null 2>&1 || true
LATEST_TAG="$(git tag -l 'sdk-v*' --sort=-v:refname | head -n1)"

if [[ -n "${LATEST_TAG}" ]]; then
  BASE_VERSION="${LATEST_TAG#sdk-v}"
  echo "Latest SDK tag: ${LATEST_TAG}"
else
  BASE_VERSION="${CURRENT_VERSION%-SNAPSHOT}"
  echo "No existing sdk-v tags found; using base version ${BASE_VERSION}"
fi

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

echo "--- :label: Creating tag sdk-v${NEW_VERSION}"
git tag "sdk-v${NEW_VERSION}"

echo "--- :arrow_up: Pushing commit and tag"
git push "${PUSH_URL}" "sdk-v${NEW_VERSION}"

echo "+++ :white_check_mark: Tagged sdk-v${NEW_VERSION} and pushed"

