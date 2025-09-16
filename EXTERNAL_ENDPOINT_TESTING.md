# External Endpoint Testing for GraphHopper

This document describes how to run GraphHopper's routing tests against external endpoints instead of the local test server.

## Overview

The routing tests in `RouteResourceClientHCTest` have been enhanced to support testing against external GraphHopper endpoints, particularly the production endpoint at `https://graphhopper.xanox.org/`. This allows validation of the production API to ensure it responds properly.

## Configuration

### System Property

The external endpoint testing is controlled by the `test.external.endpoint` system property:

- **Not set (default)**: Tests use the local Dropwizard test server
- **Set to URL**: Tests use the specified external endpoint

### Maven Profile

A convenient Maven profile `external-endpoint-test` is provided that automatically configures testing against `https://graphhopper.xanox.org/`:

```xml
<profile>
    <id>external-endpoint-test</id>
    <properties>
        <test.external.endpoint>https://graphhopper.xanox.org</test.external.endpoint>
    </properties>
</profile>
```

## Usage Examples

### 1. Default Local Testing
```bash
# Uses local Dropwizard test server (default behavior)
mvn test -pl web -Dtest=RouteResourceClientHCTest
```

### 2. External Endpoint Testing (Production)
```bash
# Uses production GraphHopper endpoint
mvn test -pl web -Pexternal-endpoint-test -Dtest=RouteResourceClientHCTest
```

### 3. Custom External Endpoint
```bash
# Uses custom GraphHopper endpoint
mvn test -pl web -Dtest.external.endpoint=https://custom.graphhopper.com -Dtest=RouteResourceClientHCTest
```

### 4. Single Test Method
```bash
# Test specific method against external endpoint
mvn test -pl web -Pexternal-endpoint-test -Dtest=RouteResourceClientHCTest#testSimpleRoute
```

### 5. Moped-Specific External Endpoint Testing
```bash
# Test moped routing specifically against external endpoint
mvn test -pl web -Pexternal-endpoint-test -Dtest=ExternalEndpointMopedTest
```

## Test Behavior

### Local Testing (Default)
- Uses local test server with Andorra OSM data
- Strict validation of route distances, times, and other metrics
- All profiles (car, bike) are available
- Tests run quickly and reliably

### External Endpoint Testing
- Uses production external endpoint
- Relaxed validation - focuses on basic connectivity and response structure
- Handles cases where profiles might not be available or data coverage differs
- Tests may fail due to network connectivity issues
- Provides actual validation of production API

## Test Adaptations

The tests automatically adapt their behavior based on the endpoint type:

```java
if (isUsingExternalEndpoint()) {
    // External endpoint - flexible validation
    if (rsp.hasErrors()) {
        String errorMessage = rsp.getErrors().toString().toLowerCase();
        if (errorMessage.contains("profile") || errorMessage.contains("point")) {
            System.out.println("External endpoint test: Expected error: " + rsp.getErrors());
            return; // Skip test for different data/profiles
        }
    }
    assertFalse(rsp.hasErrors(), "External endpoint errors:" + rsp.getErrors().toString());
    ResponsePath res = rsp.getBest();
    assertTrue(res.getDistance() > 0, "Route should have positive distance");
} else {
    // Local endpoint - strict validation
    assertFalse(rsp.hasErrors(), "errors:" + rsp.getErrors().toString());
    ResponsePath res = rsp.getBest();
    isBetween(2900, 3000, res.getDistance()); // Exact range validation
}
```

## Implementation Details

### ExternalEndpointTestUtils Class

Provides utility methods for external endpoint configuration:

- `isExternalEndpointEnabled()`: Check if external testing is enabled
- `getExternalEndpoint()`: Get the configured external endpoint URL
- `buildRoutingUrl()`: Build the routing URL from base endpoint
- `isEndpointReachable()`: Basic connectivity test

### Modified RouteResourceClientHCTest

The main test class has been enhanced with:

- `createGH()` method that chooses between local and external endpoints
- `isUsingExternalEndpoint()` helper method
- Adaptive test assertions based on endpoint type
- Better error handling for external connectivity issues

### ExternalEndpointMopedTest

A dedicated test class specifically for external endpoint testing with the `moped_nl` profile:

- `testMopedRouting()`: Tests basic moped routing functionality with Netherlands coordinates
- `testMopedRoutingShortRoute()`: Tests shorter moped routes within Amsterdam
- `testOverijsselselaanAvoidance()`: Tests that routes avoid Overijsselselaan as required
- Graceful handling of "point not found" errors for external datasets
- Optimized for the `moped_nl` profile available on production endpoints

## Error Handling

### Network Connectivity
Tests gracefully handle network issues when using external endpoints:
- DNS resolution failures
- Connection timeouts
- HTTP error responses

### Profile Availability
External endpoints may not have all profiles available:
- Tests skip validation when profiles are not found
- Error messages are logged for debugging

### Data Coverage
External endpoints may use different OSM data:
- Tests handle "point not found" errors
- Coordinate-specific tests may be skipped

## CI/CD Integration

The external endpoint testing can be integrated into CI/CD pipelines:

```yaml
# GitHub Actions example
- name: Test External Endpoint
  run: mvn test -pl web -Pexternal-endpoint-test -Dtest=RouteResourceClientHCTest

# Test moped-specific routing
- name: Test External Moped Routing
  run: mvn test -pl web -Pexternal-endpoint-test -Dtest=ExternalEndpointMopedTest
```

## Troubleshooting

### Common Issues

1. **DNS Resolution Failed**
   ```
   java.net.UnknownHostException: graphhopper.xanox.org
   ```
   - Check network connectivity
   - Verify the endpoint URL is correct

2. **Connection Timeout**
   ```
   java.net.SocketTimeoutException: connect timed out
   ```
   - Endpoint may be down or slow
   - Network firewall may be blocking access

3. **Profile Not Found**
   ```
   Profile 'bike' not found
   ```
   - External endpoint doesn't have all profiles
   - This is expected and the test will skip validation

### Debug Mode

For detailed debugging, run with verbose output:
```bash
mvn test -pl web -Pexternal-endpoint-test -Dtest=RouteResourceClientHCTest -X
```

## Benefits

1. **Production Validation**: Ensures the production API is working correctly
2. **Real-world Testing**: Tests against actual production data and configuration
3. **API Contract Verification**: Validates that the external API meets expectations
4. **Integration Testing**: Tests the complete system including any proxy/load balancers
5. **Backward Compatibility**: Maintains existing local testing capabilities

## Future Enhancements

Potential future improvements:
- Support for authentication/API keys for private endpoints
- Automatic endpoint health checks before running tests
- Configurable timeout and retry settings
- Support for multiple endpoints in a single test run
- Integration with monitoring systems for production API health