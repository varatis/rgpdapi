 #!/bin/bash

# ./.platforms/ci/zap.sh http://env-deploy.oreco.fr:4200/
source .platforms/bootstrap.sh

if [[ $# -lt 1 ]] ; then
    echo "Usage: $0 <site-url>"
    exit 1
fi

 DEP_ENV=$1
 SITE_URL=""
 DO_ZAPPROXY=true
 if [ "$DEP_ENV" == "local" ]; then
   # URL du site à analyser
   SITE_URL="http://localhost:8080/"
 elif [ "$DEP_ENV" == "demo" ]; then
   # URL du site à analyser
   SITE_URL="https://demo.minds-rgpd-api.minds.k8s/"
 elif [ "$DEP_ENV" == "valid" ]; then
   # URL du site à analyser
   SITE_URL="https://valid.minds-rgpd-api.minds.k8s/"
 else
   DO_ZAPPROXY=false
 fi

if [ -z "$SITE_URL" ]; then
  echo "No site URL defined for environment: $DEP_ENV"
  exit 1
fi
SITE_URL= "$SITE_URL/v3/api-docs"
if [ "$DO_ZAPPROXY" ]; then
  # Répertoire où sont générés les rapport
  OUTPUT_DIR=$WORKSPACE/.zap
  rm -rf $OUTPUT_DIR
  mkdir $OUTPUT_DIR

  # Paramètre Docker pour ZAP
  DOCKER_NAME="zap_proxy"
  DOCKER_WORKDIR="/zap/wrk"
  DOCKER_IMAGE_EXTRA_OPTS="-v $OUTPUT_DIR:$DOCKER_WORKDIR:rw -t"
  DOCKER_IMAGE_NAME="ghcr.io/zaproxy/zaproxy:stable"
  DOCKER_COMMAND="zap-baseline.py -t $SITE_URL -g gen.conf -r zap.html -w zap.md -l PASS -I -d"
  echo "Command: $DOCKER_COMMAND"

  echo "Execute Zap analysis"
  curl -v $SITE_URL
  dockerRun "$DOCKER_NAME" "$DOCKER_WORKDIR" "$DOCKER_IMAGE_EXTRA_OPTS" "$DOCKER_IMAGE_NAME" "$DOCKER_COMMAND"
  echo "Zap analysis done"

  # Vérifier si le rapport Markdown a été généré
  if [ -f "$OUTPUT_DIR/zap.md" ]; then
    echo "Converting ZAP Markdown report to PDF using Docker"

    # Vérifier si Docker est installé
    if ! command -v docker &> /dev/null; then
      echo "Docker is required but not installed. Please install Docker to convert Markdown to PDF."
      echo "Warning: Cannot generate PDF without Docker. Skipping PDF generation."
    else
      # Utiliser l'image Docker plass/mdtopdf pour convertir le Markdown en PDF
      echo "Running Docker container plass/mdtopdf to convert Markdown to PDF (verbose mode)"

      # Chemin absolu du répertoire de sortie pour le montage Docker
      ABSOLUTE_OUTPUT_DIR="$(cd "$OUTPUT_DIR" && pwd)"

      # Exécuter la commande Docker avec le répertoire courant monté
      docker pull plass/mdtopdf

      # Conversion du Markdown en PDF
      echo "Converting $OUTPUT_DIR/zap.md to PDF"
      docker run --rm -v "$ABSOLUTE_OUTPUT_DIR:/workdir" plass/mdtopdf mdtopdf zap.md

      if [ -f "$OUTPUT_DIR/zap.pdf" ]; then
        echo "PDF report successfully generated at $OUTPUT_DIR/zap.pdf"
      else
        echo "Error: PDF generation failed. Check Docker execution."
      fi
    fi
  else
    echo "Warning: ZAP Markdown report not found at $OUTPUT_DIR/zap.md"
  fi
fi

# It will exit with codes of:
#	0:	Success
#	1:	At least 1 FAIL
#	2:	At least one WARN and no FAILs
#	3:	Any other failure
