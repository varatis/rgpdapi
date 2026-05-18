#!/bin/bash

# ./.platforms/ci/run.sh
source .platforms/ci/bootstrap.sh

# Options DOcker
IMAGE_EXTRA_OPTS="-v ${WORKSPACE}:${WORKDIR} -v $USER_HOME/.m2:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock"

# Variables provenantes du jenkinsfile qui proviennent elles mêmes de l'interface Jenkins
echo "SKIP_TU=${SKIP_TU}"
echo "SKIP_TI=${SKIP_TI}"

# Selon les flags, il execute les tests
MVN_OPTIONS=""


if [ "$SKIP_TU" = "true" ]; then
    echo "Skipping unit tests, running integration tests only"
    MVN_OPTIONS=" -DskipTests "
fi

if [ "$SKIP_TI" = "true" ]; then
    echo "Skipping integration tests, running unit tests only"
    MVN_OPTIONS="$MVN_OPTIONS -DskipITs "
fi

echo "Running build with commands mvn verify -PIntegrationTests $MVN_OPTIONS"
docker run --rm \
   $IMAGE_EXTRA_OPTS \
   -w  ${WORKDIR}\
   srv-nexus.domaine.local:18444/outillage/maven:3.9.5-jdk21-dind \
   sh -c "mvn verify -PIntegrationTests $MVN_OPTIONS"

sudo chown -R jenkins:jenkins target/
ls -l

# FIXME Why all this complexity ?
