#!/bin/sh
# Pulls the published image on a DigitalOcean Droplet and recreates its container.
# The Droplet must have Docker installed and a runtime .env file at DO_DEPLOY_PATH/.env.

set -eu

: "${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME is required}"
: "${DOCKERHUB_TOKEN:?DOCKERHUB_TOKEN is required}"
: "${DO_DROPLET_HOST:?DO_DROPLET_HOST is required}"
: "${DO_SSH_PRIVATE_KEY:?DO_SSH_PRIVATE_KEY is required}"
: "${DO_SSH_KNOWN_HOSTS:?DO_SSH_KNOWN_HOSTS is required}"

DOCKER_IMAGE_TAG="${DOCKER_IMAGE_TAG:-isaaccmartin151/parkinglotapi-app}"
DO_SSH_USER="${DO_SSH_USER:-root}"
DO_DEPLOY_PATH="${DO_DEPLOY_PATH:-/opt/parkinglotapi}"
DO_CONTAINER_NAME="${DO_CONTAINER_NAME:-parkinglotapi-app}"
DO_HOST_PORT="${DO_HOST_PORT:-80}"

mkdir -p "${HOME}/.ssh"
chmod 700 "${HOME}/.ssh"
SSH_KEY_PATH="${HOME}/.ssh/digitalocean-deploy-key"
KNOWN_HOSTS_PATH="${HOME}/.ssh/digitalocean-known-hosts"
printf '%s\n' "${DO_SSH_PRIVATE_KEY}" > "${SSH_KEY_PATH}"
printf '%s\n' "${DO_SSH_KNOWN_HOSTS}" > "${KNOWN_HOSTS_PATH}"
chmod 600 "${SSH_KEY_PATH}" "${KNOWN_HOSTS_PATH}"

SSH_TARGET="${DO_SSH_USER}@${DO_DROPLET_HOST}"

shell_quote() {
  printf "'%s'" "$(printf '%s' "$1" | sed "s/'/'\\\\''/g")"
}

echo "--- :lock: Logging in to Docker Hub on the Droplet"
LOGIN_COMMAND="docker login --username $(shell_quote "${DOCKERHUB_USERNAME}") --password-stdin"
printf '%s' "${DOCKERHUB_TOKEN}" | ssh \
  -i "${SSH_KEY_PATH}" \
  -o IdentitiesOnly=yes \
  -o StrictHostKeyChecking=yes \
  -o "UserKnownHostsFile=${KNOWN_HOSTS_PATH}" \
  "${SSH_TARGET}" "${LOGIN_COMMAND}"

echo "--- :droplet: Deploying ${DOCKER_IMAGE_TAG} to ${SSH_TARGET}"
REMOTE_COMMAND="bash -s -- $(shell_quote "${DO_DEPLOY_PATH}") $(shell_quote "${DO_CONTAINER_NAME}") $(shell_quote "${DO_HOST_PORT}") $(shell_quote "${DOCKER_IMAGE_TAG}")"
ssh \
  -i "${SSH_KEY_PATH}" \
  -o IdentitiesOnly=yes \
  -o StrictHostKeyChecking=yes \
  -o "UserKnownHostsFile=${KNOWN_HOSTS_PATH}" \
  "${SSH_TARGET}" "${REMOTE_COMMAND}" <<'REMOTE'
set -eu
deploy_path=$1
container_name=$2
host_port=$3
image=$4

if [ ! -f "${deploy_path}/.env" ]; then
  echo "Missing runtime environment file: ${deploy_path}/.env" >&2
  exit 1
fi

docker pull "${image}"
if docker container inspect "${container_name}" >/dev/null 2>&1; then
  docker rm -f "${container_name}"
fi
docker run -d \
  --name "${container_name}" \
  --restart unless-stopped \
  --env-file "${deploy_path}/.env" \
  -p "${host_port}:8080" \
  "${image}"
REMOTE

echo "+++ :white_check_mark: DigitalOcean deployment completed"
