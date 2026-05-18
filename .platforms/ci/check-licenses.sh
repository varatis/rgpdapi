#!/bin/bash

# ./.platforms/ci/run.sh
source .platforms/ci/bootstrap.sh
# package du projet
IMAGE_EXTRA_OPTS="-v ${WORKSPACE}:${WORKDIR} -v $USER_HOME/.m2:/root/.m2"
EXCLUDED_LICENSES_URL="http://srv-nexus.domaine.local:8081/repository/raw-creative/check-licenses.sh/license-maven-plugin/contaminating-licenses.txt"
dockerRun "${PROJECT_NAME}-mvn-builder" "${WORKDIR}" "${IMAGE_EXTRA_OPTS}" "srv-nexus.domaine.local:18444/outillage/maven:3.9.5-jdk21" "mvn  license:add-third-party -Dlicense.excludedLicenses="${EXCLUDED_LICENSES_URL}