# Live Deployment Testing

This directory contains scripts and workflows for testing GraphHopper servers, both local containerized deployments and remote live endpoints.

## Files

### `.github/workflows/test-live-deployment.yml`
GitHub Action workflow that tests GraphHopper endpoints with focus on external validation. Features include:

- **External endpoint testing**: Defaults to testing `https://graphhopper.xanox.org` automatically
- **Local deployment testing**: Can test local server by setting `test_endpoint` to "localhost"
- **Custom endpoint testing**: Can be configured to test any GraphHopper endpoint
- **Smart dependency management**: Automatically detects and installs required tools (`curl`, `jq`) using available package managers
- **Cloudflare protection handling**: Properly recognizes Cloudflare challenge pages as valid responses
- **External endpoint routing tests**: Runs Java-based `ExternalEndpointMopedTest` to validate moped routing functionality
- Container status and health checks (local only)
- Server endpoint validation
- Moped profile availability verification
- Basic routing functionality testing
- **Specific route validation**: Tests that routes from `53.116614,5.781391` to `53.211454,5.803086` do NOT use Overijsselselaan

### `test-live-server.sh`
Standalone script that can test both local containers and remote endpoints with flexible configuration.

#### Usage:
```bash
# Test localhost with defaults (container: graphhopper, ports: 8989/8990)
./test-live-server.sh

# Custom container and ports (local testing)
./test-live-server.sh my-graphhopper 9000 9001

# Test external endpoint
./test-live-server.sh '' '' '' https://graphhopper.xanox.org

# Test custom endpoint with specific port
./test-live-server.sh graphhopper 8989 8990 http://server:8989

# Show help
./test-live-server.sh --help
```

#### Remote Endpoint Examples:
```bash
# HTTPS endpoint (standard port 443)
./test-live-server.sh '' '' '' https://graphhopper.xanox.org

# HTTP endpoint (standard port 80)
./test-live-server.sh '' '' '' http://myserver.com

# Custom port
./test-live-server.sh '' '' '' http://192.168.1.100:8989
```

## Testing Scenarios

### 1. Automatic External Testing (GitHub Action)
- **Default behavior**: Tests the production endpoint `https://graphhopper.xanox.org` automatically
- Triggers automatically after successful deployment
- Reports results in GitHub Actions UI
- Validates live production endpoint functionality
- **External endpoint routing tests**: Runs Java-based routing tests using the `moped_nl` profile to validate actual routing functionality

### 2. Manual Local Testing (GitHub Action)
- Set `test_endpoint` to "localhost" to test deployed containers
- Tests the deployed container on the docker host via SSH
- Useful for validating local deployments
- Fails the workflow if any tests fail

### 3. Manual Custom Endpoint Testing (GitHub Action)
- Manually triggered via workflow_dispatch with custom `test_endpoint`
- Can test any external GraphHopper endpoint
- Runs directly on GitHub Actions runner (no SSH required)
- Useful for validating different environments
- **Java routing tests**: Includes comprehensive routing tests using `ExternalEndpointMopedTest` for moped profile validation

### 3. Manual Local Testing (Shell Script)
- Run directly on the docker host
- Provides detailed colored output
- Useful for debugging deployment issues
- Can be run independently of GitHub Actions

### 4. Manual External Testing (Shell Script)
- Test any external GraphHopper endpoint
- No docker or SSH dependencies required
- Can be run from any machine with curl and jq
- Ideal for endpoint validation and debugging

## Test Coverage

All testing methods cover:

1. **Server Endpoints**
   - Health endpoint (`/health`)
   - Info endpoint (`/info`)
   - Admin endpoint (`/healthcheck`) if accessible (localhost only)

2. **Moped Profile Validation**
   - Verify `moped_nl` profile is available
   - Check `moped_access` encoded values are loaded
   - Validate profile configuration

3. **Routing Functionality**
   - Basic routing with generic coordinates
   - Netherlands-specific routing validation
   - **Critical test**: Ensure routes avoid Overijsselselaan

4. **Route Validation**
   - Test coordinates: `53.116614,5.781391` → `53.211454,5.803086`
   - Requirement: Route must NOT use Overijsselselaan
   - Analyzes route instructions to verify street avoidance

5. **Container Health** (localhost only)
   - Container running status
   - Resource usage monitoring
   - Log analysis for errors

## Dependencies

- `curl` - HTTP requests to GraphHopper API (automatically detected and installed if missing)
- `jq` - JSON parsing and validation (automatically detected and installed if missing)
- `docker` - Container management (localhost testing only)

The workflow automatically detects if dependencies are available and attempts installation using the appropriate package manager (`apt-get`, `yum`, `dnf`, or `apk`).

## Error Handling

Both scripts include comprehensive error handling:
- Timeout protection for HTTP requests
- Graceful handling of missing data or unreachable endpoints
- **Cloudflare challenge page recognition**: Endpoints behind Cloudflare protection are correctly identified as accessible
- Clear error messages and debugging information
- Exit codes that reflect test results
- Automatic fallback methods for different endpoint types
- **Multi-package manager support**: Automatic detection and installation of dependencies across different Linux distributions

## Integration with CI/CD

### Local Deployment Pipeline
The GitHub Action workflow integrates with the existing deployment pipeline:
- Automatically triggered after successful deployment
- Uses the same SSH credentials as deployment
- Provides feedback on deployment quality
- Can prevent broken deployments from going unnoticed

### External Endpoint Validation
The workflow automatically tests the production endpoint `https://graphhopper.xanox.org`:
- No manual triggering required for production testing
- Runs directly on GitHub Actions infrastructure
- Validates production endpoint after each deployment
- Can be overridden to test custom endpoints when needed

## Monitoring Route Quality

The specific route validation ensures that the moped routing engine properly respects Netherlands road access rules and avoids inappropriate routes like Overijsselselaan for the given coordinates, regardless of whether testing locally or remotely.