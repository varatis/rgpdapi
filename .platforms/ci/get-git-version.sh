#!/bin/bash

# Generate git-version.json if it doesn't exist
"$(dirname "$0")/git-version.sh"


# Extract shortHash from git-version.json
if [ -f "git-version.json" ]; then
    # Use jq if available, otherwise use sed/grep fallback
    if command -v jq >/dev/null 2>&1; then
        SHORT_HASH=$(jq -r '.shortHash' git-version.json)
    else
        # Fallback without jq
        SHORT_HASH=$(grep '"shortHash"' git-version.json | sed 's/.*"shortHash": *"\([^"]*\)".*/\1/')
    fi
    echo "$SHORT_HASH"
else
    # Fallback to git command
    git rev-parse --short HEAD
fi
