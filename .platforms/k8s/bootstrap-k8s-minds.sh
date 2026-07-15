#!/bin/bash

# Variables liées au fichier Kubeconfig à utiliser
#KUBECONFIG="$HOME/.kube/kubeconfig-interne-admin"
#KCONFIG="$USER_HOME/.kube/kubeconfig-interne-admin"
#MY_KUBECONFIG="--kubeconfig=$HOME/.kube/kubeconfig-interne-admin"
#ENV_KUBECONFIG="-e KUBECONFIG=$HOME/.kube/kubeconfig-interne-admin"
MY_KUBECONFIG="--kubeconfig=$KUBECONFIG"

cat $KUBECONFIG

# Récupération du nom du pod "tools-cli" si nécessaire
TOOLS_CLI_POD_NAME=`kubectl $MY_KUBECONFIG -n minds get pods --selector=app.kubernetes.io/name=tools-cli --output=jsonpath={.items.metadata.name}`
