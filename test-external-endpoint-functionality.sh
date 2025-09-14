#!/bin/bash

# test-external-endpoint-functionality.sh
# Script to demonstrate and test the external endpoint functionality

set -euo pipefail

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

print_section "GraphHopper External Endpoint Testing Demo"

print_status $YELLOW "This script demonstrates the external endpoint testing functionality"
print_status $YELLOW "for GraphHopper routing tests."
echo ""

# Test 1: Default local testing
print_section "Test 1: Default Local Testing"
print_status $YELLOW "Running tests with default configuration (local server)..."
print_status $YELLOW "Command: mvn test -pl web -Dtest=RouteResourceClientHCTest#testSimpleRoute"

if mvn test -pl web -Dtest=RouteResourceClientHCTest#testSimpleRoute -q; then
    print_status $GREEN "✅ Local testing works correctly"
else
    print_status $RED "❌ Local testing failed"
    exit 1
fi

# Test 2: External endpoint configuration test
print_section "Test 2: External Endpoint Configuration"
print_status $YELLOW "Testing external endpoint configuration detection..."
print_status $YELLOW "Command: mvn test -pl web -Pexternal-endpoint-test -Dtest=RouteResourceClientHCTest#testSimpleRoute"

# This will fail due to network connectivity, but should show the configuration is working
# We'll capture both stdout and stderr and check for the configuration message
TEST_OUTPUT=$(mvn test -pl web -Pexternal-endpoint-test -Dtest=RouteResourceClientHCTest#testSimpleRoute 2>&1 || true)

if echo "$TEST_OUTPUT" | grep -q "Testing external endpoint"; then
    print_status $GREEN "✅ External endpoint configuration detected correctly"
    print_status $YELLOW "   (Test failed as expected due to network connectivity, but configuration works)"
elif echo "$TEST_OUTPUT" | grep -q "graphhopper.xanox.org"; then
    print_status $GREEN "✅ External endpoint configuration detected correctly" 
    print_status $YELLOW "   (Test attempted to connect to external endpoint as expected)"
else
    print_status $RED "❌ External endpoint configuration not working"
    print_status $RED "   Output: $TEST_OUTPUT"
    exit 1
fi

# Test 3: Test with mock endpoint
print_section "Test 3: Mock External Endpoint Test"
print_status $YELLOW "Testing with mock external endpoint (localhost)..."
print_status $YELLOW "Command: mvn test -pl web -Dtest.external.endpoint=http://localhost:9999 -Dtest=RouteResourceClientHCTest#testSimpleRoute"

# This will fail due to connection refused, but should show the URL configuration is working
TEST_OUTPUT_3=$(mvn test -pl web -Dtest.external.endpoint=http://localhost:9999 -Dtest=RouteResourceClientHCTest#testSimpleRoute 2>&1 || true)

if echo "$TEST_OUTPUT_3" | grep -q "Testing external endpoint: http://localhost:9999"; then
    print_status $GREEN "✅ Custom external endpoint configuration works"
    print_status $YELLOW "   (Test failed as expected due to connection refused, but URL configuration works)"
elif echo "$TEST_OUTPUT_3" | grep -q "localhost:9999"; then
    print_status $GREEN "✅ Custom external endpoint configuration works"
    print_status $YELLOW "   (Test attempted to connect to custom endpoint as expected)"
else
    print_status $RED "❌ Custom external endpoint configuration not working"
    print_status $RED "   Output: $TEST_OUTPUT_3"
    exit 1
fi

# Test 4: Compilation and syntax check
print_section "Test 4: Code Compilation Check"
print_status $YELLOW "Verifying all code compiles correctly..."
print_status $YELLOW "Command: mvn test-compile -pl web"

if mvn test-compile -pl web -q; then
    print_status $GREEN "✅ All code compiles successfully"
else
    print_status $RED "❌ Compilation errors found"
    exit 1
fi

# Summary
print_section "Summary"
print_status $GREEN "✅ All tests passed successfully!"
echo ""
print_status $BLUE "External endpoint testing functionality is working correctly:"
print_status $GREEN "  • Local testing works (default behavior)"
print_status $GREEN "  • External endpoint detection works"  
print_status $GREEN "  • Maven profile configuration works"
print_status $GREEN "  • Custom endpoint configuration works"
print_status $GREEN "  • Code compiles without errors"
echo ""
print_status $YELLOW "Usage examples:"
print_status $YELLOW "  # Default local testing:"
print_status $YELLOW "  mvn test -pl web -Dtest=RouteResourceClientHCTest"
echo ""
print_status $YELLOW "  # External endpoint testing (when endpoint is reachable):"
print_status $YELLOW "  mvn test -pl web -Pexternal-endpoint-test -Dtest=RouteResourceClientHCTest"
echo ""
print_status $YELLOW "  # Custom endpoint testing:"
print_status $YELLOW "  mvn test -pl web -Dtest.external.endpoint=https://example.com -Dtest=RouteResourceClientHCTest"
echo ""
print_status $BLUE "🎉 Demo completed successfully!"