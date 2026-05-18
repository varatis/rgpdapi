#!/bin/bash

# cd workspace
# ./.platforms/k8s/deploy.sh valid
source .platforms/ci/bootstrap.sh
source .env

# Namespace Kubernetes Cible
NAMESPACE="minds"

# Environnement cible (valid, prod, etc)
DEP_ENV=$1
if [ "$DEP_ENV" = "demo" ]; then
    echo "Suppression de l'environnement de Demo"
    source .platforms/k8s/bootstrap-k8s-interne.sh
elif [ "$DEP_ENV" = "valid" ]; then
    echo "Suppression de l'environnement de Valid"
    source .platforms/k8s/bootstrap-k8s-interne.sh
elif [ "$DEP_ENV" = "develop" ]; then
    echo "Suppression de l'environnement de Develop"
    source .platforms/k8s/bootstrap-k8s-interne.sh
else
  echo "Paramètre 1 invalide - Environnement cible possible: valid / demo"
  exit 1
fi

TOOLS_NAME="${PROJECT_NAME}-${DEP_ENV}"

# Helm parameters
HELM_IMAGE_EXTRA_OPTS="-v $WORKSPACE:$WORKDIR -e KUBECONFIG=$KUBECONFIG"

# Deploy mattermost
HELM_COMMAND="helm --namespace $NAMESPACE delete $TOOLS_NAME"
dockerRun "$TOOLS_NAME-runner" "$WORKDIR" "$HELM_IMAGE_EXTRA_OPTS" "srv-nexus.domaine.local:18444/outillage/helm:3.7.2" "$HELM_COMMAND"
