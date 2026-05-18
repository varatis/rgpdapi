#!/bin/bash

# ./.platforms/ci/sonar.sh
source .env
source .platforms/ci/bootstrap.sh

echo "WORKDIR SONAR : ${WORKDIR}"

# Lancer une conteneur qui va analyser le code du projet localement et ensuite envoyer les résultats au serveur SonarQube
# Analyse du code back
BACK_SONAR_IMAGE_EXTRA_OPTS="-v ${WORKSPACE}:${WORKDIR}"
dockerRun "sonar-scanner" "${WORKDIR}" "${BACK_SONAR_IMAGE_EXTRA_OPTS}" "srv-nexus.domaine.local:18443/outillage/sonarqube-scanner:7.2" "sonar-scanner"
