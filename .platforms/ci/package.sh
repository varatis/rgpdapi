#!/bin/bash

# ./.platforms/ci/package.sh --with-push
source .platforms/ci/bootstrap.sh
source .env

# Registry Docker
DOCKER_REGISTRY="srv-nexus.domaine.local:18444"

# Package Back Docker Image
docker build -t minds-rgpd-api -f ".platforms/ci/dockerfiles/dockerfile" .
docker tag minds-rgpd-api "${DOCKER_REGISTRY}/projets/minds-rgpd/${PROJECT_NAME}:${PROJECT_VERSION}"


# Push Docker Image
# Push Docker Image
if [ "$1" = "--with-push" ]; then
  docker push "srv-nexus.domaine.local:18444/projets/minds-rgpd/${PROJECT_NAME}:${PROJECT_VERSION}"
fi
