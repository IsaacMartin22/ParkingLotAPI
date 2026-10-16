#!/bin/sh
# Deploys the API behind Caddy with automatic HTTPS on a DigitalOcean Droplet.
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
DO_API_DOMAIN="${DO_API_DOMAIN:-api.isaaccmart.in}"

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
REMOTE_COMMAND="bash -s -- $(shell_quote "${DO_DEPLOY_PATH}") $(shell_quote "${DO_CONTAINER_NAME}") $(shell_quote "${DO_API_DOMAIN}") $(shell_quote "${DOCKER_IMAGE_TAG}")"
ssh \
  -i "${SSH_KEY_PATH}" \
  -o IdentitiesOnly=yes \
  -o StrictHostKeyChecking=yes \
  -o "UserKnownHostsFile=${KNOWN_HOSTS_PATH}" \
  "${SSH_TARGET}" "${REMOTE_COMMAND}" <<'REMOTE'
set -eu
deploy_path=$1
container_name=$2
api_domain=$3
image=$4
network_name="${container_name}-network"
proxy_name="${container_name}-caddy"

if [ ! -f "${deploy_path}/.env" ]; then
  echo "Missing runtime environment file: ${deploy_path}/.env" >&2
  exit 1
fi

if [[ ! "${api_domain}" =~ ^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,63}$ ]]; then
  echo "DO_API_DOMAIN must be a DNS hostname, without a scheme, port or path" >&2
  exit 1
fi

docker pull "${image}"
docker pull caddy:2-alpine
if ! docker network inspect "${network_name}" >/dev/null 2>&1; then
  docker network create "${network_name}"
fi

cat > "${deploy_path}/Caddyfile" <<'CADDY'
{$API_DOMAIN} {
  reverse_proxy api:8080 {
    # Send SSE events immediately rather than buffering them.
    flush_interval -1
  }
}
CADDY

docker run --rm \
  --env "API_DOMAIN=${api_domain}" \
  --mount "type=bind,source=${deploy_path}/Caddyfile,target=/etc/caddy/Caddyfile,readonly" \
  caddy:2-alpine caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile

if docker container inspect "${container_name}" >/dev/null 2>&1; then
  docker rm -f "${container_name}"
fi
docker run -d \
  --name "${container_name}" \
  --restart unless-stopped \
  --network "${network_name}" \
  --network-alias api \
  --env-file "${deploy_path}/.env" \
  --env SERVER_FORWARD_HEADERS_STRATEGY=framework \
  "${image}"

if docker container inspect "${proxy_name}" >/dev/null 2>&1; then
  docker rm -f "${proxy_name}"
fi
docker run -d \
  --name "${proxy_name}" \
  --restart unless-stopped \
  --network "${network_name}" \
  --env "API_DOMAIN=${api_domain}" \
  --mount "type=bind,source=${deploy_path}/Caddyfile,target=/etc/caddy/Caddyfile,readonly" \
  --mount "type=volume,source=${proxy_name}-data,target=/data" \
  --mount "type=volume,source=${proxy_name}-config,target=/config" \
  -p 80:80 \
  -p 443:443 \
  caddy:2-alpine
REMOTE

echo "+++ :white_check_mark: DigitalOcean deployment completed"
