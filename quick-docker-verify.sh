#!/bin/bash

# Simplified Docker verification script for CI/CD or quick testing
# This demonstrates that the moped_nl profile error is resolved in Docker deployments

set -euo pipefail

DOCKER_IMAGE=${DOCKER_IMAGE:-"ghcr.io/xanox1/graphhopper_mpd:latest"}
CONTAINER_NAME="gh-moped-test-$(date +%s)"

echo "🔍 Testing Docker deployment for moped_nl profile fix..."
echo "📦 Docker Image: $DOCKER_IMAGE"

# Cleanup function
cleanup() {
    echo "🧹 Cleaning up..."
    docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
    docker rm "$CONTAINER_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

# Start container
echo "🚀 Starting container..."
if ! docker run -d --name "$CONTAINER_NAME" -p 8989:8989 "$DOCKER_IMAGE" >/dev/null 2>&1; then
    echo "❌ Failed to start container"
    exit 1
fi

# Wait for startup
echo "⏳ Waiting for server startup..."
for i in {1..24}; do  # 2 minute timeout
    if curl -sf http://localhost:8989/health >/dev/null 2>&1; then
        echo "✅ Server is running"
        break
    fi
    if [ $i -eq 24 ]; then
        echo "❌ Server failed to start within 2 minutes"
        echo "📋 Container logs:"
        docker logs "$CONTAINER_NAME" 2>&1 | tail -20
        exit 1
    fi
    sleep 5
done

# Check for moped errors in logs
echo "🔍 Checking Docker logs for moped errors..."
if docker logs "$CONTAINER_NAME" 2>&1 | grep -q "Could not create weighting for profile.*moped_nl"; then
    echo "❌ FAIL: Found moped_nl profile error in Docker logs!"
    docker logs "$CONTAINER_NAME" 2>&1 | grep -A5 -B5 "moped_nl"
    exit 1
fi

if docker logs "$CONTAINER_NAME" 2>&1 | grep -q "Cannot compile expression.*Not a boolean expression"; then
    echo "❌ FAIL: Found boolean expression error in Docker logs!"
    docker logs "$CONTAINER_NAME" 2>&1 | grep -A5 -B5 "boolean expression"
    exit 1
fi

# Verify moped profile availability
echo "🔍 Verifying moped_nl profile availability..."
RESPONSE=$(curl -s http://localhost:8989/info)

if echo "$RESPONSE" | jq -e '.profiles[] | select(.name == "moped_nl")' >/dev/null 2>&1; then
    echo "✅ moped_nl profile is available"
else
    echo "❌ FAIL: moped_nl profile not found"
    echo "Available profiles: $(echo "$RESPONSE" | jq -r '.profiles[].name' | tr '\n' ', ')"
    exit 1
fi

if echo "$RESPONSE" | jq -e '.encoded_values.moped_access' >/dev/null 2>&1; then
    echo "✅ moped_access encoded value is available"
    ENUM_VALUES=$(echo "$RESPONSE" | jq -r '.encoded_values.moped_access | join(", ")')
    echo "   Values: [$ENUM_VALUES]"
else
    echo "❌ FAIL: moped_access encoded value not found"
    exit 1
fi

echo ""
echo "🎉 SUCCESS: Docker deployment verification passed!"
echo "✅ moped_nl profile loads without errors"
echo "✅ moped_access enum values are properly configured"
echo "✅ No boolean expression compilation errors found"
echo ""
echo "The issue 'Could not create weighting for profile: moped_nl' has been resolved in Docker deployment."