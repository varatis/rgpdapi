#!/bin/bash

# Récupération du fichier bootstrap-commons.sh
source <(curl -s http://srv-nexus.domaine.local:8081/repository/raw-creative/fr/creative/ci/bootstrap-commons.sh)

##########################################################################
# Variables
##########################################################################

# Répertoire de travail "Docker"
WORKDIR=$(clean_path "/code")

# Nom du projet
PROJECT_NAME="minds-rgpd-api"