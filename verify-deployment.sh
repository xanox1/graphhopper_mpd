#!/bin/bash

# GraphHopper Docker Deployment Verification Script
# This script tests the Docker deployment to ensure it's working correctly

set -e

CONTAINER_NAME=${1:-graphhopper}
HOST=${2:-localhost}
PORT=${3:-8989}

echo "🔍 Verifying GraphHopper Docker deployment..."
echo "Container: $CONTAINER_NAME"
echo "Host: $HOST"
echo "Port: $PORT"
echo ""

# Check if container is running
echo "1. Checking if container is running..."
if docker ps --filter name=$CONTAINER_NAME --format "table {{.Names}}\t{{.Status}}" | grep -q $CONTAINER_NAME; then
    echo "✅ Container '$CONTAINER_NAME' is running"
    docker ps --filter name=$CONTAINER_NAME --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
else
    echo "❌ Container '$CONTAINER_NAME' is not running"
    echo "Available containers:"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    exit 1
fi

echo ""

# Test health endpoint
echo "2. Testing health endpoint..."
if curl -f -s http://$HOST:$PORT/health > /dev/null; then
    HEALTH_RESPONSE=$(curl -s http://$HOST:$PORT/health)
    echo "✅ Health endpoint responding: $HEALTH_RESPONSE"
else
    echo "❌ Health endpoint not responding"
    exit 1
fi

echo ""

# Test info endpoint
echo "3. Testing info endpoint..."
if curl -f -s http://$HOST:$PORT/info > /dev/null; then
    INFO_RESPONSE=$(curl -s http://$HOST:$PORT/info | jq -r '.version // "unknown"' 2>/dev/null || echo "unknown")
    echo "✅ Info endpoint responding. GraphHopper version: $INFO_RESPONSE"
else
    echo "❌ Info endpoint not responding"
    exit 1
fi

echo ""

# Check container logs for errors
echo "4. Checking container logs for startup issues..."
if docker logs $CONTAINER_NAME --tail 20 2>&1 | grep -i error | head -5; then
    echo "⚠️  Found error messages in logs (showing last 5):"
    docker logs $CONTAINER_NAME --tail 20 2>&1 | grep -i error | head -5
else
    echo "✅ No obvious errors in recent logs"
fi

echo ""

# Test admin endpoint (if accessible)
echo "5. Testing admin endpoint (port 8990)..."
if curl -f -s http://$HOST:8990/healthcheck > /dev/null 2>&1; then
    echo "✅ Admin endpoint responding on port 8990"
else
    echo "ℹ️  Admin endpoint not accessible (this is normal if not exposed)"
fi

echo ""
echo "✅ GraphHopper Docker deployment verification completed successfully!"
echo ""
echo "🌐 Access the GraphHopper Maps UI at: http://$HOST:$PORT/"
echo "📖 API Documentation: http://$HOST:$PORT/"
echo "🔧 Health Check: http://$HOST:$PORT/health"
echo "ℹ️  Server Info: http://$HOST:$PORT/info"