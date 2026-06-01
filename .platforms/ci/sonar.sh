#!/bin/bash

# ./.platforms/ci/sonar.sh
source .env
source .platforms/ci/bootstrap.sh

# Lancer une conteneur qui va analyser le code du projet localement et ensuite envoyer les résultats au serveur SonarQube
# Analyse du code back
BACK_SONAR_IMAGE_EXTRA_OPTS="-v ${WORKSPACE}:${WORKDIR} -e sonar.branch.name=${BRANCH_NAME}"
IS_CRITICAL=$3

echo "Is sonarqube analysis critical: [$IS_CRITICAL]"

if [ "$IS_CRITICAL" == "true" ]; then
    BACK_SONAR_IMAGE_EXTRA_OPTS="$BACK_SONAR_IMAGE_EXTRA_OPTS -e sonar.qualitygate.wait=true"
fi
dockerRun "sonar-scanner" "${WORKDIR}" "${BACK_SONAR_IMAGE_EXTRA_OPTS}" "srv-nexus.domaine.local:18443/outillage/sonarqube-scanner:7.2" "sonar-scanner"
