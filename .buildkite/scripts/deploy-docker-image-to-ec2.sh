#!/usr/bin/env bash
# deploy-docker-image-to-ec2.sh
# Connects to an EC2 host over SSH, pulls the published Docker image, and
# recreates the API container with the configured runtime environment.
#
# Required environment variables:
#   DOCKERHUB_USERNAME
#   DOCKERHUB_TOKEN
#   EC2_HOST
#   EC2_SSH_PRIVATE_KEY
#
# Optional environment variables:
#   DOCKER_IMAGE_TAG                 Defaults to isaaccmartin151/parkinglotapi-app
#   EC2_DEPLOY_USER                 Defaults to ec2-user
#   EC2_DEPLOY_PATH                 Defaults to /opt/parkinglotapi
#   EC2_CONTAINER_NAME              Defaults to parkinglotapi-app
#   EC2_APP_PORT                    Defaults to 8080
#   EC2_HOST_PORT                   Defaults to 8080
#   SPRING_PROFILES_ACTIVE          Defaults to prod
#   NEW_RELIC_APP_NAME              Defaults to parking-lot-api
#   POSTGRES_DATASOURCE_URL
#   POSTGRES_DATASOURCE_USERNAME
#   POSTGRES_DATASOURCE_PASSWORD
#   NEW_RELIC_LICENSE_KEY

set -euo pipefail

: "${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME is required for EC2 image pull}"
: "${DOCKERHUB_TOKEN:?DOCKERHUB_TOKEN is required for EC2 image pull}"
: "${EC2_HOST:?EC2_HOST is required}"
: "${EC2_SSH_PRIVATE_KEY:?EC2_SSH_PRIVATE_KEY is required}"

DOCKER_IMAGE_TAG="${DOCKER_IMAGE_TAG:-isaaccmartin151/parkinglotapi-app}"
EC2_DEPLOY_USER="${EC2_DEPLOY_USER:-ec2-user}"
EC2_DEPLOY_PATH="${EC2_DEPLOY_PATH:-/opt/parkinglotapi}"
EC2_CONTAINER_NAME="${EC2_CONTAINER_NAME:-parkinglotapi-app}"
EC2_APP_PORT="${EC2_APP_PORT:-8080}"
EC2_HOST_PORT="${EC2_HOST_PORT:-8080}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
NEW_RELIC_APP_NAME="${NEW_RELIC_APP_NAME:-parking-lot-api}"

echo "--- :lock: Preparing SSH key"
mkdir -p "${HOME}/.ssh"
chmod 700 "${HOME}/.ssh"
SSH_KEY_PATH="${HOME}/.ssh/ec2-deploy-key"
printf '%s\n' "${EC2_SSH_PRIVATE_KEY}" > "${SSH_KEY_PATH}"
chmod 600 "${SSH_KEY_PATH}"

echo "--- :rocket: Deploying ${DOCKER_IMAGE_TAG} to ${EC2_DEPLOY_USER}@${EC2_HOST}"
ssh -i "${SSH_KEY_PATH}" -o StrictHostKeyChecking=no "${EC2_DEPLOY_USER}@${EC2_HOST}" \
  "DOCKERHUB_USERNAME='${DOCKERHUB_USERNAME}' \
  DOCKERHUB_TOKEN='${DOCKERHUB_TOKEN}' \
  DOCKER_IMAGE_TAG='${DOCKER_IMAGE_TAG}' \
  EC2_DEPLOY_PATH='${EC2_DEPLOY_PATH}' \
  EC2_CONTAINER_NAME='${EC2_CONTAINER_NAME}' \
  EC2_APP_PORT='${EC2_APP_PORT}' \
  EC2_HOST_PORT='${EC2_HOST_PORT}' \
  SPRING_PROFILES_ACTIVE='${SPRING_PROFILES_ACTIVE}' \
  NEW_RELIC_APP_NAME='${NEW_RELIC_APP_NAME}' \
  NEW_RELIC_LICENSE_KEY='${NEW_RELIC_LICENSE_KEY:-}' \
  POSTGRES_DATASOURCE_URL='${POSTGRES_DATASOURCE_URL:-}' \
  POSTGRES_DATASOURCE_USERNAME='${POSTGRES_DATASOURCE_USERNAME:-}' \
  POSTGRES_DATASOURCE_PASSWORD='${POSTGRES_DATASOURCE_PASSWORD:-}' \
  bash -se" <<'EOF'
set -euo pipefail

mkdir -p "${EC2_DEPLOY_PATH}"

docker login -u "${DOCKERHUB_USERNAME}" --password-stdin <<LOGIN
${DOCKERHUB_TOKEN}
LOGIN

docker pull "${DOCKER_IMAGE_TAG}"

docker rm -f "${EC2_CONTAINER_NAME}" >/dev/null 2>&1 || true

docker run -d \
  --name "${EC2_CONTAINER_NAME}" \
  --restart unless-stopped \
  -p "${EC2_HOST_PORT}:${EC2_APP_PORT}" \
  -e SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE}" \
  -e NEW_RELIC_APP_NAME="${NEW_RELIC_APP_NAME}" \
  -e NEW_RELIC_LICENSE_KEY="${NEW_RELIC_LICENSE_KEY}" \
  -e SPRING_DATASOURCE_URL="${POSTGRES_DATASOURCE_URL}" \
  -e SPRING_DATASOURCE_USERNAME="${POSTGRES_DATASOURCE_USERNAME}" \
  -e SPRING_DATASOURCE_PASSWORD="${POSTGRES_DATASOURCE_PASSWORD}" \
  "${DOCKER_IMAGE_TAG}"
EOF

echo "+++ :white_check_mark: EC2 deployment completed"
