#!/bin/bash

# cd workspace
# ./.platforms/k8s/deploy.sh valid
source .platforms/ci/bootstrap.sh
source .env

# Namespace Kubernetes Cible
NAMESPACE="minds"

# Environnement cible (valid, prod, etc)
DEP_ENV=$1
echo "env = $DEP_ENV"

if [ "$DEP_ENV" = "demo" ]; then
    echo "Déploiement sur l'environnement de Demo"
    source .platforms/k8s/bootstrap-k8s-minds.sh
elif [ "$DEP_ENV" = "valid" ]; then
    echo "Déploiement sur l'environnement de Valid"
    source .platforms/k8s/bootstrap-k8s-minds.sh
elif [ "$DEP_ENV" = "int" ]; then
    echo "Déploiement sur l'environnement d'Integration"
    source .platforms/k8s/bootstrap-k8s-minds.sh
else
  echo "Paramètre 1 invalide - Environnement cible possible: int / valid / demo"
  exit 1
fi


sh("printenv | sort -n")

# Default namespace & kubeconfig
TOOLS_NAME="${PROJECT_NAME}-${DEP_ENV}"
# Helm parameters
HELM_IMAGE_EXTRA_OPTS="-v $WORKSPACE:$WORKDIR -e KUBECONFIG=$KUBECONFIG"
echo "HELM_IMAGE_EXTRA_OPTS ${HELM_IMAGE_EXTRA_OPTS}"

# Deploy Application
# HELM_COMMAND="helm upgrade --recreate-pods --install --debug --wait --namespace $NAMESPACE $TOOLS_NAME -f $WORKDIR/.platforms/k8s/helm/values-$DEP_ENV.yaml --set back.image.tag=$PROJECT_VERSION ./.platforms/k8s/helm"
HELM_COMMAND="helm upgrade --install --debug --wait --namespace $NAMESPACE $TOOLS_NAME -f $WORKDIR/.platforms/k8s/values-$DEP_ENV.yaml --set back.image.tag=$PROJECT_VERSION ./.platforms/k8s/helm"
dockerRun "$TOOLS_NAME-runner" "$WORKDIR" "$HELM_IMAGE_EXTRA_OPTS" "srv-nexus.domaine.local:18444/outillage/helm:3.12.2" "kubectl get pods -n minds"
dockerRun "$TOOLS_NAME-runner" "$WORKDIR" "$HELM_IMAGE_EXTRA_OPTS" "srv-nexus.domaine.local:18444/outillage/helm:3.12.2" "$HELM_COMMAND"
