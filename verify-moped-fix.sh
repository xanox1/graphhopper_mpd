#!/bin/bash

# Simple verification script to demonstrate the moped_nl profile issue has been resolved
# This script runs without Docker and uses the local build

set -euo pipefail

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

print_status $YELLOW "GraphHopper MPD Moped Profile Fix Verification"
print_status $YELLOW "==============================================="

# Verify project builds correctly
print_status $YELLOW "Step 1: Building project..."
if mvn clean compile -DskipTests -q; then
    print_status $GREEN "✓ Project builds successfully"
else
    print_status $RED "✗ Project failed to build"
    exit 1
fi

# Run the specific tests that reproduce and validate the fix
print_status $YELLOW "Step 2: Running moped profile validation tests..."
if mvn test -Dtest="MopedProfileValidationTest,MopedAccessValidationTest,MopedNlModelIntegrationTest" -pl core -q; then
    print_status $GREEN "✓ All moped validation tests pass"
else
    print_status $RED "✗ Moped validation tests failed"
    exit 1
fi

# Build the web application
print_status $YELLOW "Step 3: Building web application..."
if mvn clean install -DskipTests -q; then
    print_status $GREEN "✓ Web application built successfully"
else
    print_status $RED "✗ Web application build failed"
    exit 1
fi

# Start server in background and test moped_nl profile
print_status $YELLOW "Step 4: Testing server startup with moped_nl profile..."

# Start server in background
java -Ddw.graphhopper.datareader.file=core/files/andorra.osm.pbf \
     -jar web/target/graphhopper-web-*.jar server config-example.yml >/dev/null 2>&1 &
SERVER_PID=$!

# Function to cleanup server
cleanup_server() {
    if [ ! -z "${SERVER_PID:-}" ]; then
        kill $SERVER_PID >/dev/null 2>&1 || true
        wait $SERVER_PID 2>/dev/null || true
    fi
}
trap cleanup_server EXIT

# Wait for server to start
print_status $YELLOW "Waiting for server to start..."
sleep 15

# Check if server is responding
if curl -sf http://localhost:8989/health >/dev/null 2>&1; then
    print_status $GREEN "✓ Server started successfully"
else
    print_status $RED "✗ Server failed to start"
    exit 1
fi

# Verify moped_nl profile is available
print_status $YELLOW "Step 5: Verifying moped_nl profile availability..."
INFO_RESPONSE=$(curl -sf http://localhost:8989/info 2>/dev/null || echo "")

if [ -z "$INFO_RESPONSE" ]; then
    print_status $RED "✗ Could not retrieve server info"
    exit 1
fi

# Check if moped_nl profile is listed
if echo "$INFO_RESPONSE" | grep -q '"name":"moped_nl"'; then
    print_status $GREEN "✓ moped_nl profile is available"
else
    print_status $RED "✗ moped_nl profile is not available"
    echo "Available profiles: $(echo "$INFO_RESPONSE" | grep -o '"profiles":\[[^]]*\]')"
    exit 1
fi

# Check if moped_access encoded value is available
if echo "$INFO_RESPONSE" | grep -q '"moped_access":\['; then
    print_status $GREEN "✓ moped_access encoded value is available"
    MOPED_ACCESS_VALUES=$(echo "$INFO_RESPONSE" | grep -o '"moped_access":\[[^]]*\]' | sed 's/"moped_access":\[//; s/\]$//')
    print_status $GREEN "  Values: [$MOPED_ACCESS_VALUES]"
else
    print_status $RED "✗ moped_access encoded value is not available"
    exit 1
fi

# Verify specific enum values are present
EXPECTED_VALUES=("MISSING" "NO" "YES" "DESIGNATED" "USE_SIDEPATH")
for value in "${EXPECTED_VALUES[@]}"; do
    if echo "$INFO_RESPONSE" | grep -q "\"$value\""; then
        print_status $GREEN "  ✓ Enum value '$value' found"
    else
        print_status $RED "  ✗ Enum value '$value' missing"
        exit 1
    fi
done

print_status $YELLOW "Step 6: Final verification..."
print_status $GREEN "=============================="
print_status $GREEN "SUCCESS: All verification checks passed!"
print_status $GREEN ""
print_status $GREEN "The moped_nl profile error has been RESOLVED:"
print_status $GREEN "• Project builds without errors"
print_status $GREEN "• Moped validation tests pass"
print_status $GREEN "• Server starts successfully with moped_nl profile"
print_status $GREEN "• moped_access encoded values are properly loaded"
print_status $GREEN "• All expected enum values (MISSING, NO, YES, DESIGNATED, USE_SIDEPATH) are available"
print_status $GREEN ""
print_status $GREEN "The original error 'Cannot compile expression: File priority entry, Line 1, Column 6: Not a boolean expression'"
print_status $GREEN "has been fixed by changing '!moped_access' to 'moped_access == MISSING || moped_access == NO'"
print_status $GREEN "in the moped_nl_model.json configuration file."
print_status $GREEN "=============================="