#!/bin/bash

# Docker Deployment Verification Script for GraphHopper MPD Moped Profile
# This script verifies that the moped_nl profile error has been resolved after deployment

set -euo pipefail

# Configuration
DOCKER_IMAGE=${DOCKER_IMAGE:-"ghcr.io/xanox1/graphhopper_mpd:latest"}
CONTAINER_NAME=${CONTAINER_NAME:-"graphhopper-moped-test"}
TIMEOUT=${TIMEOUT:-120}  # seconds to wait for server startup
SERVER_PORT=${SERVER_PORT:-8989}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}[$(date '+%Y-%m-%d %H:%M:%S')] ${message}${NC}"
}

# Function to cleanup container
cleanup_container() {
    if docker ps -q -f name="${CONTAINER_NAME}" | grep -q .; then
        print_status $YELLOW "Stopping container ${CONTAINER_NAME}..."
        docker stop "${CONTAINER_NAME}" >/dev/null 2>&1 || true
    fi
    if docker ps -aq -f name="${CONTAINER_NAME}" | grep -q .; then
        print_status $YELLOW "Removing container ${CONTAINER_NAME}..."
        docker rm "${CONTAINER_NAME}" >/dev/null 2>&1 || true
    fi
}

# Function to check if server is healthy
wait_for_server() {
    local max_attempts=$((TIMEOUT / 5))
    local attempt=1
    
    print_status $YELLOW "Waiting for GraphHopper server to start (timeout: ${TIMEOUT}s)..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -sf "http://localhost:${SERVER_PORT}/health" >/dev/null 2>&1; then
            print_status $GREEN "Server is healthy!"
            return 0
        fi
        
        print_status $YELLOW "Attempt ${attempt}/${max_attempts}: Server not ready yet..."
        sleep 5
        ((attempt++))
    done
    
    print_status $RED "Server failed to start within ${TIMEOUT} seconds"
    return 1
}

# Function to check Docker logs for errors
check_docker_logs() {
    print_status $YELLOW "Checking Docker logs for moped profile errors..."
    
    # Check for the specific error mentioned in the problem statement
    if docker logs "${CONTAINER_NAME}" 2>&1 | grep -q "Could not create weighting for profile: 'moped_nl'"; then
        print_status $RED "FAILURE: Found moped_nl profile error in logs!"
        docker logs "${CONTAINER_NAME}" 2>&1 | grep -A 5 -B 5 "Could not create weighting for profile"
        return 1
    fi
    
    if docker logs "${CONTAINER_NAME}" 2>&1 | grep -q "Cannot compile expression.*Not a boolean expression"; then
        print_status $RED "FAILURE: Found boolean expression compilation error in logs!"
        docker logs "${CONTAINER_NAME}" 2>&1 | grep -A 5 -B 5 "Cannot compile expression"
        return 1
    fi
    
    # Check for general startup errors
    if docker logs "${CONTAINER_NAME}" 2>&1 | grep -q "ERROR.*moped"; then
        print_status $RED "FAILURE: Found moped-related errors in logs!"
        docker logs "${CONTAINER_NAME}" 2>&1 | grep "ERROR.*moped"
        return 1
    fi
    
    print_status $GREEN "No moped profile errors found in Docker logs"
    return 0
}

# Function to verify moped_nl profile availability
verify_moped_profile() {
    print_status $YELLOW "Verifying moped_nl profile is available..."
    
    local info_response
    info_response=$(curl -sf "http://localhost:${SERVER_PORT}/info" 2>/dev/null || echo "")
    
    if [ -z "$info_response" ]; then
        print_status $RED "FAILURE: Could not retrieve server info"
        return 1
    fi
    
    # Check if moped_nl profile is listed using multiple methods for robustness
    MOPED_PROFILE_FOUND="false"
    
    # Method 1: Direct jq selection
    if echo "$info_response" | jq -e '.profiles[] | select(.name == "moped_nl")' >/dev/null 2>&1; then
        print_status $GREEN "SUCCESS: moped_nl profile is available (jq select method)"
        MOPED_PROFILE_FOUND="true"
    fi
    
    # Method 2: Extract profile names and grep (fallback for older jq versions)
    if [ "$MOPED_PROFILE_FOUND" = "false" ]; then
        PROFILE_NAMES=$(echo "$info_response" | jq -r '.profiles[].name' 2>/dev/null || echo "")
        if echo "$PROFILE_NAMES" | grep -q "^moped_nl$"; then
            print_status $GREEN "SUCCESS: moped_nl profile is available (grep method)"
            MOPED_PROFILE_FOUND="true"
        fi
    fi
    
    # Method 3: Check using contains (another fallback)
    if [ "$MOPED_PROFILE_FOUND" = "false" ]; then
        if echo "$info_response" | jq -r '.profiles | map(.name) | contains(["moped_nl"])' 2>/dev/null | grep -q "true"; then
            print_status $GREEN "SUCCESS: moped_nl profile is available (contains method)"
            MOPED_PROFILE_FOUND="true"
        fi
    fi
    
    if [ "$MOPED_PROFILE_FOUND" = "false" ]; then
        print_status $RED "FAILURE: moped_nl profile is not available"
        print_status $YELLOW "Available profiles:"
        echo "$info_response" | jq '.profiles' 2>/dev/null || echo "Could not parse profiles"
        return 1
    fi
    
    # Check if moped_access encoded value is available
    if echo "$info_response" | jq -e '.encoded_values.moped_access' >/dev/null 2>&1; then
        print_status $GREEN "SUCCESS: moped_access encoded value is available"
        local moped_access_values
        moped_access_values=$(echo "$info_response" | jq -r '.encoded_values.moped_access | join(", ")')
        print_status $GREEN "moped_access values: [$moped_access_values]"
    else
        print_status $RED "FAILURE: moped_access encoded value is not available"
        return 1
    fi
    
    return 0
}

# Function to test moped routing functionality
test_moped_routing() {
    print_status $YELLOW "Testing moped_nl routing functionality..."
    
    # Using sample coordinates from Netherlands (if using Netherlands data)
    # Fall back to generic coordinates that should work with any dataset
    local test_routes=(
        "52.370216,4.895168&point=52.520008,6.083887"  # Amsterdam to Groningen (Netherlands)
        "42.50,1.52&point=42.51,1.53"                   # Generic test coordinates
    )
    
    for route in "${test_routes[@]}"; do
        print_status $YELLOW "Testing route with coordinates: $route"
        
        local route_response
        route_response=$(curl -sf "http://localhost:${SERVER_PORT}/route?point=${route}&profile=moped_nl&ch.disable=true" 2>/dev/null || echo "")
        
        if [ -z "$route_response" ]; then
            print_status $YELLOW "No response for route coordinates: $route"
            continue
        fi
        
        # Check if we got a successful route or an acceptable error (like point not found)
        if echo "$route_response" | jq -e '.paths[0].distance' >/dev/null 2>&1; then
            local distance
            distance=$(echo "$route_response" | jq -r '.paths[0].distance')
            print_status $GREEN "SUCCESS: Moped routing worked! Distance: ${distance}m"
            return 0
        elif echo "$route_response" | jq -e '.message' | grep -q "Cannot find point"; then
            print_status $YELLOW "Point not found (acceptable - dataset might not cover these coordinates)"
            continue
        elif echo "$route_response" | jq -e '.message' | grep -q "moped"; then
            print_status $RED "FAILURE: Moped-specific routing error:"
            echo "$route_response" | jq '.message' 2>/dev/null || echo "$route_response"
            return 1
        fi
    done
    
    print_status $YELLOW "No successful routes found, but no moped-specific errors detected"
    return 0
}

# Main verification function
main() {
    print_status $YELLOW "Starting Docker deployment verification for GraphHopper MPD moped profile..."
    print_status $YELLOW "Docker image: ${DOCKER_IMAGE}"
    print_status $YELLOW "Container name: ${CONTAINER_NAME}"
    print_status $YELLOW "Server port: ${SERVER_PORT}"
    print_status $YELLOW "Timeout: ${TIMEOUT}s"
    
    # Check if Docker is available
    if ! command -v docker >/dev/null 2>&1; then
        print_status $RED "FAILURE: Docker is not installed or not in PATH"
        exit 1
    fi
    
    # Check if jq is available for JSON parsing
    if ! command -v jq >/dev/null 2>&1; then
        print_status $RED "FAILURE: jq is not installed or not in PATH (required for JSON parsing)"
        exit 1
    fi
    
    # Cleanup any existing container
    cleanup_container
    
    print_status $YELLOW "Starting GraphHopper container..."
    
    # Start the container
    if ! docker run -d \
        --name "${CONTAINER_NAME}" \
        -p "${SERVER_PORT}:8989" \
        -p 8990:8990 \
        "${DOCKER_IMAGE}" >/dev/null 2>&1; then
        print_status $RED "FAILURE: Could not start Docker container"
        exit 1
    fi
    
    print_status $GREEN "Container started successfully"
    
    # Setup cleanup trap
    trap cleanup_container EXIT
    
    # Wait for server to start
    if ! wait_for_server; then
        print_status $RED "FAILURE: Server failed to start"
        print_status $YELLOW "Container logs:"
        docker logs "${CONTAINER_NAME}" 2>&1 | tail -50
        exit 1
    fi
    
    # Check logs for errors
    if ! check_docker_logs; then
        print_status $RED "FAILURE: Found errors in Docker logs"
        exit 1
    fi
    
    # Verify moped profile
    if ! verify_moped_profile; then
        print_status $RED "FAILURE: Moped profile verification failed"
        exit 1
    fi
    
    # Test routing functionality
    if ! test_moped_routing; then
        print_status $RED "FAILURE: Moped routing test failed"
        exit 1
    fi
    
    print_status $GREEN "SUCCESS: All verification checks passed!"
    print_status $GREEN "The moped_nl profile error has been resolved in the Docker deployment."
    
    return 0
}

# Show usage if requested
if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    echo "Docker Deployment Verification Script for GraphHopper MPD"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Environment variables:"
    echo "  DOCKER_IMAGE      Docker image to test (default: ghcr.io/xanox1/graphhopper_mpd:latest)"
    echo "  CONTAINER_NAME    Container name for testing (default: graphhopper-moped-test)"
    echo "  TIMEOUT           Timeout in seconds for server startup (default: 120)"
    echo "  SERVER_PORT       Local port to bind server to (default: 8989)"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Use defaults"
    echo "  DOCKER_IMAGE=my-image:latest $0       # Use custom image"
    echo "  TIMEOUT=180 $0                        # Use longer timeout"
    exit 0
fi

# Run main function
main