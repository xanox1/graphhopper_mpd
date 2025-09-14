#!/bin/bash

# Live GraphHopper Server Test Script
# This script tests the actual deployed GraphHopper server including specific routing validation
# Usage: ./test-live-server.sh [container_name] [server_port] [admin_port]

set -euo pipefail

# Configuration with defaults
CONTAINER_NAME=${1:-"graphhopper"}
SERVER_PORT=${2:-"8989"}
ADMIN_PORT=${3:-"8990"}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}[$(date '+%Y-%m-%d %H:%M:%S')] ${message}${NC}"
}

# Function to print section headers
print_section() {
    local message=$1
    echo ""
    print_status $BLUE "============================================"
    print_status $BLUE "$message"
    print_status $BLUE "============================================"
}

# Function to check dependencies
check_dependencies() {
    print_section "Checking Dependencies"
    
    local missing_deps=()
    
    if ! command -v docker >/dev/null 2>&1; then
        missing_deps+=("docker")
    fi
    
    if ! command -v curl >/dev/null 2>&1; then
        missing_deps+=("curl")
    fi
    
    if ! command -v jq >/dev/null 2>&1; then
        missing_deps+=("jq")
    fi
    
    if [ ${#missing_deps[@]} -ne 0 ]; then
        print_status $RED "❌ Missing required dependencies: ${missing_deps[*]}"
        print_status $YELLOW "Please install the missing dependencies and try again"
        exit 1
    fi
    
    print_status $GREEN "✅ All dependencies available"
}

# Function to test container status
test_container_status() {
    print_section "Container Status Check"
    
    if docker ps --filter name=$CONTAINER_NAME --format "table {{.Names}}\t{{.Status}}" | grep -q $CONTAINER_NAME; then
        print_status $GREEN "✅ Container '$CONTAINER_NAME' is running"
        
        # Show detailed container info
        print_status $YELLOW "Container details:"
        docker ps --filter name=$CONTAINER_NAME --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}\t{{.RunningFor}}"
        
        # Show container resource usage
        print_status $YELLOW "Container resource usage:"
        docker stats $CONTAINER_NAME --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}"
    else
        print_status $RED "❌ Container '$CONTAINER_NAME' is not running"
        print_status $YELLOW "Available containers:"
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        return 1
    fi
}

# Function to check container logs
check_container_logs() {
    print_section "Container Logs Analysis"
    
    print_status $YELLOW "Checking recent logs for errors..."
    
    # Check for startup errors
    if docker logs $CONTAINER_NAME --tail 50 2>&1 | grep -i "error\|exception\|failed\|cannot" | head -10; then
        print_status $YELLOW "⚠️  Found potential error messages in logs:"
        docker logs $CONTAINER_NAME --tail 50 2>&1 | grep -i "error\|exception\|failed\|cannot" | head -10
        echo ""
    else
        print_status $GREEN "✅ No obvious error messages in recent logs"
    fi
    
    # Check for successful startup message
    if docker logs $CONTAINER_NAME 2>&1 | grep -q "Started Server"; then
        print_status $GREEN "✅ Server startup message found in logs"
    else
        print_status $YELLOW "⚠️  Server startup message not found - server may still be starting"
    fi
    
    # Check for moped-specific initialization
    if docker logs $CONTAINER_NAME 2>&1 | grep -qi "moped"; then
        print_status $GREEN "✅ Moped-related initialization found in logs"
        print_status $YELLOW "Moped-related log entries:"
        docker logs $CONTAINER_NAME 2>&1 | grep -i "moped" | tail -5
    else
        print_status $YELLOW "⚠️  No moped-specific log entries found"
    fi
}

# Function to test server endpoints
test_server_endpoints() {
    print_section "Server Endpoint Testing"
    
    # Test health endpoint
    print_status $YELLOW "Testing health endpoint..."
    HEALTH_TIMEOUT=60
    HEALTH_ATTEMPTS=$((HEALTH_TIMEOUT / 5))
    
    for i in $(seq 1 $HEALTH_ATTEMPTS); do
        if curl -f -s --max-time 10 http://localhost:${SERVER_PORT}/health > /dev/null; then
            HEALTH_RESPONSE=$(curl -s --max-time 10 http://localhost:${SERVER_PORT}/health)
            print_status $GREEN "✅ Health endpoint responding: $HEALTH_RESPONSE"
            break
        else
            print_status $YELLOW "Attempt $i/$HEALTH_ATTEMPTS: Health endpoint not ready..."
            if [ $i -eq $HEALTH_ATTEMPTS ]; then
                print_status $RED "❌ Health endpoint failed after $HEALTH_TIMEOUT seconds"
                return 1
            fi
            sleep 5
        fi
    done
    
    # Test info endpoint with retry logic
    print_status $YELLOW "Testing info endpoint..."
    INFO_RESPONSE=""
    
    for attempt in 1 2 3; do
        if curl -f -s --max-time 15 http://localhost:${SERVER_PORT}/info > /dev/null; then
            INFO_RESPONSE=$(curl -s --max-time 15 http://localhost:${SERVER_PORT}/info)
            if [ -n "$INFO_RESPONSE" ] && echo "$INFO_RESPONSE" | jq . >/dev/null 2>&1; then
                break
            else
                print_status $YELLOW "Attempt $attempt: Invalid JSON response, retrying..."
                sleep 2
            fi
        else
            print_status $YELLOW "Attempt $attempt: Info endpoint not responding, retrying..."
            sleep 2
        fi
        
        if [ $attempt -eq 3 ]; then
            print_status $RED "❌ Info endpoint failed after 3 attempts"
            return 1
        fi
    done
    
    VERSION=$(echo "$INFO_RESPONSE" | jq -r '.version // "unknown"' 2>/dev/null || echo "unknown")
    BUILD_DATE=$(echo "$INFO_RESPONSE" | jq -r '.build_date // "unknown"' 2>/dev/null || echo "unknown")
    print_status $GREEN "✅ Info endpoint responding"
    print_status $GREEN "   GraphHopper version: $VERSION"
    print_status $GREEN "   Build date: $BUILD_DATE"
    
    # Test admin endpoint
    print_status $YELLOW "Testing admin endpoint..."
    if curl -f -s --max-time 10 http://localhost:${ADMIN_PORT}/healthcheck > /dev/null 2>&1; then
        print_status $GREEN "✅ Admin endpoint responding on port ${ADMIN_PORT}"
    else
        print_status $YELLOW "ℹ️  Admin endpoint not accessible (this is normal if not exposed)"
    fi
}

# Function to test moped profile
test_moped_profile() {
    print_section "Moped Profile Validation"
    
    INFO_RESPONSE=$(curl -s --max-time 10 http://localhost:${SERVER_PORT}/info)
    
    # Check if moped_nl profile is available using multiple methods for robustness
    MOPED_PROFILE_FOUND="false"
    
    # Method 1: Direct jq selection
    if echo "$INFO_RESPONSE" | jq -e '.profiles[] | select(.name == "moped_nl")' >/dev/null 2>&1; then
        print_status $GREEN "✅ moped_nl profile found (jq select method)"
        MOPED_PROFILE_FOUND="true"
    fi
    
    # Method 2: Extract profile names and grep (fallback for older jq versions)
    if [ "$MOPED_PROFILE_FOUND" = "false" ]; then
        PROFILE_NAMES=$(echo "$INFO_RESPONSE" | jq -r '.profiles[].name' 2>/dev/null || echo "")
        if echo "$PROFILE_NAMES" | grep -q "^moped_nl$"; then
            print_status $GREEN "✅ moped_nl profile found (grep method)"
            MOPED_PROFILE_FOUND="true"
        fi
    fi
    
    # Method 3: Check using contains (another fallback)
    if [ "$MOPED_PROFILE_FOUND" = "false" ]; then
        if echo "$INFO_RESPONSE" | jq -r '.profiles | map(.name) | contains(["moped_nl"])' 2>/dev/null | grep -q "true"; then
            print_status $GREEN "✅ moped_nl profile found (contains method)"
            MOPED_PROFILE_FOUND="true"
        fi
    fi
    
    if [ "$MOPED_PROFILE_FOUND" = "true" ]; then
        print_status $GREEN "✅ moped_nl profile is available"
        
        # Show moped profile details
        MOPED_PROFILE=$(echo "$INFO_RESPONSE" | jq '.profiles[] | select(.name == "moped_nl")' 2>/dev/null || echo '{"name": "moped_nl"}')
        print_status $YELLOW "Moped profile details:"
        echo "$MOPED_PROFILE" | jq '.' 2>/dev/null || echo "Could not parse profile details"
    else
        print_status $RED "❌ moped_nl profile is NOT available"
        print_status $YELLOW "Available profiles:"
        echo "$INFO_RESPONSE" | jq '.profiles[].name' 2>/dev/null || echo "Could not parse profiles"
        print_status $YELLOW "Raw profiles JSON:"
        echo "$INFO_RESPONSE" | jq '.profiles' 2>/dev/null || echo "Could not parse profiles JSON"
        return 1
    fi
    
    # Check if moped_access encoded value is available
    if echo "$INFO_RESPONSE" | jq -e '.encoded_values.moped_access' >/dev/null 2>&1; then
        MOPED_ACCESS_VALUES=$(echo "$INFO_RESPONSE" | jq -r '.encoded_values.moped_access | join(", ")')
        print_status $GREEN "✅ moped_access encoded value available: [$MOPED_ACCESS_VALUES]"
    else
        print_status $RED "❌ moped_access encoded value is NOT available"
        return 1
    fi
    
    # Show all encoded values for reference
    print_status $YELLOW "All available encoded values:"
    echo "$INFO_RESPONSE" | jq -r '.encoded_values | to_entries[] | "  \(.key): [\(.value | join(", "))]"' 2>/dev/null || echo "Could not parse encoded values"
}

# Function to test basic routing
test_basic_routing() {
    print_section "Basic Routing Functionality"
    
    print_status $YELLOW "Testing basic moped routing..."
    
    # Test with generic coordinates that should work with any dataset
    BASIC_ROUTE_URL="http://localhost:${SERVER_PORT}/route?point=42.50,1.52&point=42.51,1.53&profile=moped_nl&ch.disable=true"
    
    if BASIC_ROUTE_RESPONSE=$(curl -sf --max-time 30 "$BASIC_ROUTE_URL" 2>/dev/null); then
        if echo "$BASIC_ROUTE_RESPONSE" | jq -e '.paths[0].distance' >/dev/null 2>&1; then
            DISTANCE=$(echo "$BASIC_ROUTE_RESPONSE" | jq -r '.paths[0].distance')
            TIME=$(echo "$BASIC_ROUTE_RESPONSE" | jq -r '.paths[0].time // 0')
            print_status $GREEN "✅ Basic moped routing works!"
            print_status $GREEN "   Distance: ${DISTANCE}m"
            print_status $GREEN "   Time: ${TIME}ms"
        elif echo "$BASIC_ROUTE_RESPONSE" | jq -e '.message' | grep -q "Cannot find point"; then
            print_status $YELLOW "⚠️  Basic routing: Point not found (acceptable - dataset may not cover test coordinates)"
        else
            print_status $RED "❌ Basic routing failed with response:"
            echo "$BASIC_ROUTE_RESPONSE" | jq '.' 2>/dev/null || echo "$BASIC_ROUTE_RESPONSE"
            return 1
        fi
    else
        print_status $RED "❌ Basic routing request failed"
        return 1
    fi
}

# Function to test specific route validation
test_specific_route_validation() {
    print_section "Specific Route Validation (Overijsselselaan Avoidance)"
    
    # Coordinates from the problem statement: 53.116614,5.781391 to 53.211454,5.803086
    SPECIFIC_FROM="53.116614,5.781391"
    SPECIFIC_TO="53.211454,5.803086"
    SPECIFIC_ROUTE_URL="http://localhost:${SERVER_PORT}/route?point=${SPECIFIC_FROM}&point=${SPECIFIC_TO}&profile=moped_nl&ch.disable=true&instructions=true"
    
    print_status $YELLOW "Testing route from $SPECIFIC_FROM to $SPECIFIC_TO"
    print_status $YELLOW "This route should NOT use Overijsselselaan"
    
    if SPECIFIC_ROUTE_RESPONSE=$(curl -sf --max-time 30 "$SPECIFIC_ROUTE_URL" 2>/dev/null); then
        if echo "$SPECIFIC_ROUTE_RESPONSE" | jq -e '.paths[0].distance' >/dev/null 2>&1; then
            DISTANCE=$(echo "$SPECIFIC_ROUTE_RESPONSE" | jq -r '.paths[0].distance')
            TIME=$(echo "$SPECIFIC_ROUTE_RESPONSE" | jq -r '.paths[0].time // 0')
            print_status $GREEN "✅ Specific route calculation successful!"
            print_status $GREEN "   Distance: ${DISTANCE}m"
            print_status $GREEN "   Time: ${TIME}ms"
            
            # Check if route uses Overijsselselaan (the route should NOT use this street)
            if echo "$SPECIFIC_ROUTE_RESPONSE" | jq -r '.paths[0].instructions[].text // ""' | grep -qi "overijsselselaan"; then
                print_status $RED "❌ ROUTE VALIDATION FAILED: Route uses Overijsselselaan!"
                print_status $RED "   The routing agent should never route over Overijsselselaan for these coordinates."
                print_status $YELLOW "   Route instructions mentioning Overijsselselaan:"
                echo "$SPECIFIC_ROUTE_RESPONSE" | jq -r '.paths[0].instructions[].text // ""' | grep -i "overijsselselaan" || true
                return 1
            else
                print_status $GREEN "✅ Route validation passed: Route does NOT use Overijsselselaan"
            fi
            
            # Show route instructions for verification
            print_status $YELLOW "Complete route instructions:"
            echo "$SPECIFIC_ROUTE_RESPONSE" | jq -r '.paths[0].instructions[] | "\(.distance // 0)m - \(.text // "N/A")"' | head -20 || true
            
            # Show route summary
            NUM_INSTRUCTIONS=$(echo "$SPECIFIC_ROUTE_RESPONSE" | jq -r '.paths[0].instructions | length' 2>/dev/null || echo "0")
            print_status $YELLOW "Route summary: $NUM_INSTRUCTIONS instructions, ${DISTANCE}m total distance"
            
        elif echo "$SPECIFIC_ROUTE_RESPONSE" | jq -e '.message' | grep -q "Cannot find point"; then
            print_status $YELLOW "⚠️  Specific routing: Cannot find point (dataset may not cover Netherlands coordinates)"
            print_status $YELLOW "   This is acceptable if the server is not using Netherlands OSM data"
        else
            print_status $RED "❌ Specific routing failed with response:"
            echo "$SPECIFIC_ROUTE_RESPONSE" | jq '.' 2>/dev/null || echo "$SPECIFIC_ROUTE_RESPONSE"
            return 1
        fi
    else
        print_status $RED "❌ Specific routing request failed"
        return 1
    fi
}

# Function to run all tests
run_all_tests() {
    local failed_tests=()
    
    # Run each test and track failures
    check_dependencies || failed_tests+=("dependencies")
    test_container_status || failed_tests+=("container_status")
    check_container_logs || failed_tests+=("container_logs")
    test_server_endpoints || failed_tests+=("server_endpoints")
    test_moped_profile || failed_tests+=("moped_profile")
    test_basic_routing || failed_tests+=("basic_routing")
    test_specific_route_validation || failed_tests+=("specific_route_validation")
    
    # Final summary
    print_section "Test Summary"
    
    if [ ${#failed_tests[@]} -eq 0 ]; then
        print_status $GREEN "🎉 ALL TESTS PASSED!"
        print_status $GREEN "✅ Container is running properly"
        print_status $GREEN "✅ Server endpoints are responding"
        print_status $GREEN "✅ Moped profile is working correctly"
        print_status $GREEN "✅ Routing functionality is operational"
        print_status $GREEN "✅ Route correctly avoids Overijsselselaan"
        
        print_status $BLUE "GraphHopper Server Information:"
        print_status $BLUE "🌐 Web UI: http://$(hostname -I | awk '{print $1}' 2>/dev/null || echo 'localhost'):${SERVER_PORT}/"
        print_status $BLUE "📖 API: http://$(hostname -I | awk '{print $1}' 2>/dev/null || echo 'localhost'):${SERVER_PORT}/"
        print_status $BLUE "🔧 Health: http://$(hostname -I | awk '{print $1}' 2>/dev/null || echo 'localhost'):${SERVER_PORT}/health"
        print_status $BLUE "ℹ️  Info: http://$(hostname -I | awk '{print $1}' 2>/dev/null || echo 'localhost'):${SERVER_PORT}/info"
        
        return 0
    else
        print_status $RED "❌ SOME TESTS FAILED"
        print_status $RED "Failed tests: ${failed_tests[*]}"
        return 1
    fi
}

# Show usage information
show_usage() {
    echo "Live GraphHopper Server Test Script"
    echo ""
    echo "Usage: $0 [container_name] [server_port] [admin_port]"
    echo ""
    echo "Parameters:"
    echo "  container_name    Docker container name (default: graphhopper)"
    echo "  server_port       GraphHopper server port (default: 8989)"
    echo "  admin_port        GraphHopper admin port (default: 8990)"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Use defaults"
    echo "  $0 my-graphhopper 9000 9001          # Custom container and ports"
    echo ""
    echo "This script tests:"
    echo "  - Container status and logs"
    echo "  - Server health and info endpoints"
    echo "  - Moped profile availability"
    echo "  - Basic routing functionality"
    echo "  - Specific route validation (avoiding Overijsselselaan)"
    echo ""
    echo "Dependencies: docker, curl, jq"
}

# Main execution
main() {
    if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
        show_usage
        exit 0
    fi
    
    print_section "Live GraphHopper Server Test"
    print_status $YELLOW "Container: $CONTAINER_NAME"
    print_status $YELLOW "Server port: $SERVER_PORT"
    print_status $YELLOW "Admin port: $ADMIN_PORT"
    
    if run_all_tests; then
        print_status $GREEN "Live server test completed successfully! 🎉"
        exit 0
    else
        print_status $RED "Live server test failed! ❌"
        exit 1
    fi
}

# Run main function
main "$@"