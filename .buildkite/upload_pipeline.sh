#!/usr/bin/env bash
set -euo pipefail

# Flatten multiline commit messages for safe env and metadata display.
export CI_COMMIT_MESSAGE="$(printf '%s' "${BUILDKITE_MESSAGE:-}" | tr '\n' ' ' | sed 's/[[:space:]]\+/ /g' | sed 's/^ //; s/ $//')"
export CI_BRANCH_NAME="${BUILDKITE_BRANCH:-}"
export CI_COMMIT_SHA="${BUILDKITE_COMMIT:-}"

if [[ -z "$CI_COMMIT_MESSAGE" ]]; then
  CI_COMMIT_MESSAGE="(no commit message)"
fi

CI_COMMIT_MESSAGE_SHORT="$CI_COMMIT_MESSAGE"
if [[ ${#CI_COMMIT_MESSAGE_SHORT} -gt 70 ]]; then
  CI_COMMIT_MESSAGE_SHORT="${CI_COMMIT_MESSAGE_SHORT:0:67}..."
fi

yaml_escape() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  printf '%s' "$value"
}

CI_COMMIT_MESSAGE_ESCAPED="$(yaml_escape "$CI_COMMIT_MESSAGE")"
CI_COMMIT_MESSAGE_SHORT_ESCAPED="$(yaml_escape "$CI_COMMIT_MESSAGE_SHORT")"
CI_BRANCH_NAME_ESCAPED="$(yaml_escape "$CI_BRANCH_NAME")"
CI_COMMIT_SHA_ESCAPED="$(yaml_escape "$CI_COMMIT_SHA")"

buildkite-agent meta-data set ci_commit_message "$CI_COMMIT_MESSAGE"
buildkite-agent meta-data set ci_branch_name "$CI_BRANCH_NAME"
buildkite-agent meta-data set ci_commit_sha "$CI_COMMIT_SHA"

buildkite-agent annotate "Branch: ${CI_BRANCH_NAME}<br/>Commit: ${CI_COMMIT_SHA}<br/>Message: ${CI_COMMIT_MESSAGE}" --style "info" --context "build-context"

rendered_pipeline="$(cat .buildkite/pipeline.dynamic.yml)"
rendered_pipeline="${rendered_pipeline//__CI_COMMIT_MESSAGE__/$CI_COMMIT_MESSAGE_ESCAPED}"
rendered_pipeline="${rendered_pipeline//__CI_COMMIT_MESSAGE_SHORT__/$CI_COMMIT_MESSAGE_SHORT_ESCAPED}"
rendered_pipeline="${rendered_pipeline//__CI_BRANCH_NAME__/$CI_BRANCH_NAME_ESCAPED}"
rendered_pipeline="${rendered_pipeline//__CI_COMMIT_SHA__/$CI_COMMIT_SHA_ESCAPED}"

printf '%s\n' "$rendered_pipeline" > .buildkite/pipeline.generated.yml

buildkite-agent pipeline upload ".buildkite/pipeline.generated.yml"

