#!/bin/bash

source .platforms/ci/bootstrap.sh

# Why sudo and chmod ? Should make better fix
cd target
sudo rm -rf .trivy && mkdir .trivy && chmod 777 .trivy
DOCKER_IMAGE_EXTRA_OPTS="-v ${WORKSPACE}:${WORKDIR}"

# Trivy Configuration > https://aquasecurity.github.io/trivy/v0.45/docs/references/configuration/config-file/
# --exit-code : 0 par défaut, on passe à 1 s'il y a des CVE du niveau de "--serverity"
# --severity : Affiche uniquement les CVE du niveau indiqué (plusieurs valeurs possibles séparées par des virgules: UNKNOWN | LOW | MEDIUM | HIGH | CRITICAL)
# .trivyignore: fichier à mettre à la racine du projet, permet de whitelister certaines CVE
TRIVY_CMD="trivy fs --exit-code 1 --severity HIGH,CRITICAL --format template --template "@/usr/local/share/trivy/templates/html.tpl" -o ${WORKDIR}/.trivy/trivy.html ${WORKDIR}"
dockerRun "trivy-scanner-${PROJECT_NAME}" "${WORKDIR}" "${DOCKER_IMAGE_EXTRA_OPTS}" "srv-nexus.domaine.local:18444/outillage/trivy:0.56.2" "$TRIVY_CMD"