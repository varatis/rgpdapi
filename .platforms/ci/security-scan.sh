#!/bin/bash
# api-security-scan.sh - Project-level REST API security scanner
# This is the ONLY file projects need - everything else is in the Docker image
source .platforms/ci/bootstrap.sh

# Project-specific configuration (customize per project)
SCANNER_IMAGE="srv-nexus:18444/outillage/zaproxy:stable"
# SCANNER_IMAGE="your-registry/zap-scanner:latest" # For local DEBUG
PROJECT_NAME="minds-rgpd-api"

# Base URL configuration (REQUIRED - customize for your API)
BASE_URL=""

# OAuth2 configuration (same pattern as frontend scanning)
AUTH_URL="https://sso.minds.k8s/auth/realms/minds-rgpd/protocol/openid-connect/token"

# Environment mapping with smart URL building
case "${1:-develop}" in
    "demo")
        BASE_URL="https://demo.minds-rgpd-api.minds.k8s"
        ;;
    "valid")
        BASE_URL="https://valid.minds-rgpd-api.minds.k8s"
        ;;
    *)
        echo "❌ Unknown environment: ${1}"
        echo "Available: demo, valid"
        exit 1
        ;;
esac

# Validation
if [ -z "$BASE_URL" ]; then
    echo "❌ BASE_URL not configured for environment: ${1:-develop}"
    echo "Edit this script and set BASE_URL for your application"
    exit 1
fi

ENVIRONMENT=${1:-"develop"}

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_header() {
    echo -e "${BLUE}"
    echo "🔒 Corporate API Security Scanner for $PROJECT_NAME"
    echo "Environment: $ENVIRONMENT"
    echo "Base URL: $BASE_URL"
    echo "Auto-detection: Enabled"
    echo "================================================"
    echo -e "${NC}"
}

# Validate configuration
validate_config() {
    if [ "$BASE_URL" = "https://my-api.com" ] || [ "$BASE_URL" = "https://api-staging.my-app.com" ]; then
        echo -e "${RED}❌ Please configure BASE_URL in this script${NC}"
        echo "Edit the case statement to set your actual application URLs"
        exit 1
    fi

    echo -e "${GREEN}✅ Configuration validated${NC}"
    echo "  Base URL: $BASE_URL"
    echo "  Derived URLs will be auto-detected:"
    echo "    • API endpoints: $BASE_URL/api, $BASE_URL/rest, etc."
    echo "    • OpenAPI docs: $BASE_URL/v3/api-docs, $BASE_URL/swagger.json, etc."
    echo "    • Health checks: $BASE_URL/actuator/health, $BASE_URL/health, etc."
    echo "    • Swagger UI: $BASE_URL/swagger-ui.html, etc."
}

# Create optional API endpoints configuration
create_optional_endpoints() {
    echo "📝 Checking for custom API endpoints configuration..."

    if [ ! -f "./api-endpoints.txt" ]; then
        echo -e "${YELLOW}ℹ️  Optional: Create ./api-endpoints.txt for custom endpoints${NC}"
        echo "Example format:"
        echo "  GET:/api/users"
        echo "  POST:/api/users:application/json:{\"name\":\"test\"}"
        echo "  PUT:/api/users/{id}:application/json:{\"name\":\"updated\"}"
        echo ""
        echo "The scanner will auto-detect endpoints from OpenAPI, but custom"
        echo "endpoints can supplement the auto-detection."
    else
        echo -e "${GREEN}✅ Custom endpoints file found: ./api-endpoints.txt${NC}"
        # Validate format
        local line_count=$(grep -v '^#\|^$' ./api-endpoints.txt | wc -l)
        echo "  Custom endpoints defined: $line_count"
    fi
}

# Run API security scan using corporate dockerRun method
run_api_scan() {
    echo "🚀 Starting API security scan using corporate dockerRun..."

    # Create reports directory
    mkdir -p "./security-reports"

    # Copy custom endpoints if they exist
    if [ -f "./api-endpoints.txt" ]; then
        cp "./api-endpoints.txt" "./security-reports/"
        echo -e "${GREEN}✅ Custom endpoints copied to scan environment${NC}"
    fi

    # Prepare environment variables for the scanner
    local docker_env_vars=""

    # Always pass the base URL and environment
    docker_env_vars="$docker_env_vars -e ZAP_BASE_URL=$BASE_URL"
    docker_env_vars="$docker_env_vars -e ZAP_ENVIRONMENT=$ENVIRONMENT"

    # Pass OAuth2 authentication URL
    docker_env_vars="$docker_env_vars -e ZAP_AUTH_URL=$AUTH_URL"

    # Pass credentials from Jenkins/environment (same pattern as frontend)
    if [ -n "$OAUTH_CLIENT_ID" ]; then
        echo "DEBUG: client id provided"
        docker_env_vars="$docker_env_vars -e OAUTH_CLIENT_ID=$OAUTH_CLIENT_ID"
    fi
    if [ -n "$OAUTH_CLIENT_SECRET" ]; then
        echo "DEBUG: client secret provided"
        docker_env_vars="$docker_env_vars -e OAUTH_CLIENT_SECRET=$OAUTH_CLIENT_SECRET"
    fi
    if [ -n "$ZAP_USERNAME" ]; then
        echo "DEBUG: username provided"
        docker_env_vars="$docker_env_vars -e ZAP_USERNAME=$ZAP_USERNAME"
    fi
    if [ -n "$ZAP_PASSWORD" ]; then
        echo "DEBUG: password provided"
        docker_env_vars="$docker_env_vars -e ZAP_PASSWORD=$ZAP_PASSWORD"
    fi
    echo "DEBUG: docker env var: $docker_env_vars"


    docker pull $SCANNER_IMAGE

    # Use corporate dockerRun method
    docker run --rm \
            --name "security-scanner-api" \
            --network host \
            --user "$USER_ID:$GROUP_ID" \
            -v "$(pwd)/security-reports:/zap/wrk:rw" \
            $docker_env_vars \
            $SCANNER_IMAGE \
            zap-api-scan api-scan "$ENVIRONMENT"
}

# Show configuration help
show_config_help() {
    echo "🔧 API Configuration Help"
    echo ""
    echo "This script uses smart auto-detection to minimize configuration needs!"
    echo ""
    echo "Required Configuration:"
    echo "1. Edit this script (api-security-scan.sh)"
    echo "2. Update the case statement with your base URLs:"
    echo ""
    echo '   "prod")'
    echo '       BASE_URL="https://your-api.com"'
    echo '       ;;'
    echo '   "staging")'
    echo '       BASE_URL="https://staging-api.your-company.com"'
    echo '       ;;'
    echo ""
    echo "Optional Configuration:"
    echo "3. Create api-endpoints.txt for custom endpoints:"
    echo "   GET:/api/custom-endpoint"
    echo "   POST:/api/data:application/json:{\"key\":\"value\"}"
    echo ""
    echo "4. Jenkins credentials (same as frontend scanning):"
    echo "   • Use existing zap-auth-credentials"
    echo "   • OAuth2 client ID → ZAP_USERNAME"
    echo "   • OAuth2 client secret → ZAP_PASSWORD"
    echo ""
    echo "Auto-Detection Features:"
    echo "• OpenAPI/Swagger documentation discovery"
    echo "• API base path detection (/api, /rest, etc.)"
    echo "• Spring Boot Actuator endpoints"
    echo "• Authentication method detection"
    echo "• Comprehensive endpoint coverage"
    echo ""
    echo "Run: ./api-security-scan.sh prod"
}

# Show available commands
show_help() {
    echo "🔒 Corporate API Security Scanner"
    echo ""
    echo "Usage:"
    echo "  ./api-security-scan.sh [environment]"
    echo ""
    echo "Environments:"
    echo "  demo, valid"
    echo ""
    echo "Examples:"
    echo "  ./api-security-scan.sh valid      # Scan validation environment"
    echo "  ./api-security-scan.sh prod       # Scan production environment"
    echo ""
    echo "Setup:"
    echo "  ./api-security-scan.sh config     # Show configuration help"
    echo ""
    echo "Features:"
    echo "  🔍 Auto-detects OpenAPI/Swagger documentation"
    echo "  🔍 Auto-detects API endpoints and patterns"
    echo "  🔍 Auto-detects Spring Boot Actuator endpoints"
    echo "  🔐 OAuth2 JWT authentication support"
    echo "  📊 Comprehensive API coverage reporting"
    echo "  🏢 Uses corporate dockerRun method"
    echo "  ⚡ Minimal configuration required"
    echo ""
    echo "Results will be saved in ./security-reports/"
    echo "  📄 index.html - Main security report"
    echo "  📊 api-coverage.html - API endpoint coverage"
    echo "  📋 summary.txt - Scan summary"
}

# Test connectivity and show preview
test_connectivity() {
    echo "🔍 Testing connectivity and showing detection preview..."
    echo ""

    # Test base URL
    echo "Testing base URL: $BASE_URL"
    if curl -s --connect-timeout 5 --max-time 10 "$BASE_URL" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Base URL is accessible${NC}"
    else
        echo -e "${YELLOW}⚠️  Base URL not accessible (might be normal if behind VPN)${NC}"
    fi

    # Test common API endpoints
    echo ""
    echo "Testing common API patterns:"

    local test_urls=(
        "$BASE_URL/v3/api-docs"
        "$BASE_URL/swagger-ui.html"
        "$BASE_URL/actuator/health"
        "$BASE_URL/api"
        "$BASE_URL/health"
    )

    for url in "${test_urls[@]}"; do
        echo -n "  $(basename "$url"): "
        if curl -s --connect-timeout 3 --max-time 5 "$url" | head -c 100 | grep -q "openapi\|swagger\|status\|UP\|api" 2>/dev/null; then
            echo -e "${GREEN}Found${NC}"
        else
            echo -e "${YELLOW}Not found${NC}"
        fi
    done

    echo ""
    echo "Note: Full auto-detection runs during the actual scan with more comprehensive patterns."
}

# Main execution
main() {
    case "${1:-scan}" in
        "config")
            show_config_help
            ;;
        "help"|"--help"|"-h")
            show_help
            ;;
        "test")
            print_header
            validate_config
            test_connectivity
            ;;
        *)
            print_header
            validate_config
            create_optional_endpoints
            run_api_scan
            echo ""
            echo -e "${GREEN}✅ API security scan completed!${NC}"
            echo "📄 Main report: ./security-reports/index.html"
            echo "📊 API coverage: ./security-reports/api-coverage.html"
            echo "📋 Summary: ./security-reports/summary.txt"
            echo ""
            echo "🎯 Scan highlights:"
            if [ -f "./security-reports/summary.txt" ]; then
                grep -E "OpenAPI|alerts|endpoints" "./security-reports/summary.txt" | head -5
            fi
            ;;
    esac
}

main "$@"
