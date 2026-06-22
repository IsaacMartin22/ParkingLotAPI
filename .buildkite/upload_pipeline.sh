#!/usr/bin/env bash
set -euo pipefail

# Flatten multiline commit messages for safe env and metadata display.
export CI_COMMIT_MESSAGE="$(printf '%s' "${BUILDKITE_MESSAGE:-}" | tr '\n' ' ' | sed 's/[[:space:]]\+/ /g' | sed 's/^ //; s/ $//')"
export CI_BRANCH_NAME="${BUILDKITE_BRANCH:-}"
export CI_COMMIT_SHA="${BUILDKITE_COMMIT:-}"

buildkite-agent meta-data set ci_commit_message "$CI_COMMIT_MESSAGE"
buildkite-agent meta-data set ci_branch_name "$CI_BRANCH_NAME"
buildkite-agent meta-data set ci_commit_sha "$CI_COMMIT_SHA"

buildkite-agent annotate "Branch: ${CI_BRANCH_NAME}<br/>Commit: ${CI_COMMIT_SHA}<br/>Message: ${CI_COMMIT_MESSAGE}" --style "info" --context "build-context"

buildkite-agent pipeline upload ".buildkite/pipeline.dynamic.yml"

