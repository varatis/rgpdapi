#!/bin/bash

# Get git commit information
COMMIT_HASH=$(git rev-parse HEAD)
SHORT_HASH=$(git rev-parse --short HEAD)
AUTHOR_NAME=$(git log -1 --pretty=format:'%an')
AUTHOR_EMAIL=$(git log -1 --pretty=format:'%ae')
COMMIT_DATE=$(git log -1 --pretty=format:'%ci')
COMMIT_MESSAGE=$(git log -1 --pretty=format:'%s')
BRANCH=$(git rev-parse --abbrev-ref HEAD)

# Get version from pom.xml if it exists, otherwise use default
if [ -f "pom.xml" ]; then
    VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)
    if [ -z "$VERSION" ] || [ "$VERSION" = "null object or invalid expression" ]; then
        VERSION="1.0.0"
    fi
else
    VERSION="1.0.0"
fi

# Create timestamp for version (similar to frontend)
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M")

# Generate JSON similar to frontend's git-commit-info format
cat > git-version.json << EOF
{
  "hash": "$COMMIT_HASH",
  "shortHash": "$SHORT_HASH",
  "author": "$AUTHOR_NAME <$AUTHOR_EMAIL>",
  "date": "$COMMIT_DATE",
  "message": "$COMMIT_MESSAGE",
  "branch": "$BRANCH",
  "version": "v${VERSION}-${TIMESTAMP}-${SHORT_HASH}",
  "imageversion": "v${VERSION}-${SHORT_HASH}"
}
EOF

#echo "Generated git-version.json with commit info"
