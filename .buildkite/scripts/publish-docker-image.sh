#!/bin/sh
# publish-docker-image.sh
# Builds the API service and MCP server Docker images and pushes both to Docker Hub.
#
# Required environment variables:
#   DOCKERHUB_USERNAME  – Docker Hub username
#   DOCKERHUB_TOKEN     – Docker Hub access token or password
#
# Optional environment variables:
#   DOCKER_IMAGE_TAG      – Full API image tag to build and push
#   MCP_DOCKER_IMAGE_TAG  – Full MCP server image tag to build and push

set -eu

: "${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME is required to push the Docker image}"
: "${DOCKERHUB_TOKEN:?DOCKERHUB_TOKEN is required to push the Docker image}"

DOCKER_IMAGE_TAG="${DOCKER_IMAGE_TAG:-isaaccmartin151/parkinglotapi-app}"
MCP_DOCKER_IMAGE_TAG="${MCP_DOCKER_IMAGE_TAG:-isaaccmartin151/parkinglotapi-mcp-server}"

echo "--- :whale: Logging in to Docker Hub"
echo "${DOCKERHUB_TOKEN}" | docker login -u "${DOCKERHUB_USERNAME}" --password-stdin

echo "--- :mag_right: Verifying New Relic agent files are present"
ls -la newrelic
[ -f newrelic/newrelic.jar ] || { echo "Missing newrelic/newrelic.jar in the build context"; exit 1; }
[ -f newrelic/newrelic.yml ] || { echo "Missing newrelic/newrelic.yml in the build context"; exit 1; }

echo "--- :building_construction: Building Docker image ${DOCKER_IMAGE_TAG}"
docker build -t "${DOCKER_IMAGE_TAG}" .

echo "--- :building_construction: Building MCP server Docker image ${MCP_DOCKER_IMAGE_TAG}"
docker build -f mcp-server/Dockerfile -t "${MCP_DOCKER_IMAGE_TAG}" .

# Push only after both builds succeed so the two images are always published as a matching pair.
echo "--- :outbox_tray: Pushing Docker image ${DOCKER_IMAGE_TAG}"
docker push "${DOCKER_IMAGE_TAG}"

echo "--- :outbox_tray: Pushing MCP server Docker image ${MCP_DOCKER_IMAGE_TAG}"
docker push "${MCP_DOCKER_IMAGE_TAG}"

echo "+++ :white_check_mark: Docker images published successfully"
