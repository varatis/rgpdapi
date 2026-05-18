#!/bin/bash

# ./.platforms/ci/run.sh
source .platforms/ci/bootstrap.sh

# Lancement du projet
USER_HOME=${USER_HOME} WORKSPACE=${WORKSPACE} docker-compose -f .platforms/ci/docker-compose/docker-compose-test-package.yml up
