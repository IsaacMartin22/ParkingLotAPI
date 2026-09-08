#!/bin/sh
# publish-docker-image.sh
# Builds the API service Docker image and pushes it to Docker Hub.
#
# Required environment variables:
#   DOCKERHUB_USERNAME  – Docker Hub username
#   DOCKERHUB_TOKEN     – Docker Hub access token or password
#
# Optional environment variables:
#   DOCKER_IMAGE_TAG    – Full image tag to build and push

set -eu

: "${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME is required to push the Docker image}"
: "${DOCKERHUB_TOKEN:?DOCKERHUB_TOKEN is required to push the Docker image}"

DOCKER_IMAGE_TAG="${DOCKER_IMAGE_TAG:-isaaccmartin151/parkinglotapi-app}"

echo "--- :whale: Logging in to Docker Hub"
echo "${DOCKERHUB_TOKEN}" | docker login -u "${DOCKERHUB_USERNAME}" --password-stdin

echo "--- :building_construction: Building Docker image ${DOCKER_IMAGE_TAG}"
docker build -t "${DOCKER_IMAGE_TAG}" .

echo "--- :outbox_tray: Pushing Docker image ${DOCKER_IMAGE_TAG}"
docker push "${DOCKER_IMAGE_TAG}"

echo "+++ :white_check_mark: Docker image published successfully"
