# Live Deployment Testing

This directory contains scripts and workflows for testing the actual live GraphHopper server after deployment to the docker host.

## Files

### `.github/workflows/test-live-deployment.yml`
GitHub Action workflow that automatically runs after successful deployment to test the live server. It performs comprehensive testing including:

- Container status and health checks
- Server endpoint validation
- Moped profile availability verification
- Basic routing functionality testing
- **Specific route validation**: Tests that routes from `53.116614,5.781391` to `53.211454,5.803086` do NOT use Overijsselselaan

### `test-live-server.sh`
Standalone script that can be run manually on the docker host to perform the same comprehensive tests as the GitHub Action.

#### Usage:
```bash
# Use defaults (container: graphhopper, ports: 8989/8990)
./test-live-server.sh

# Custom container and ports
./test-live-server.sh my-graphhopper 9000 9001

# Show help
./test-live-server.sh --help
```

## Testing Scenarios

### 1. Automatic Testing (GitHub Action)
- Triggers automatically after successful deployment
- Can be manually triggered via workflow_dispatch
- Reports results in GitHub Actions UI
- Fails the workflow if any tests fail

### 2. Manual Testing (Shell Script)
- Run directly on the docker host
- Provides detailed colored output
- Useful for debugging deployment issues
- Can be run independently of GitHub Actions

## Test Coverage

Both testing methods cover:

1. **Container Health**
   - Container running status
   - Resource usage monitoring
   - Log analysis for errors

2. **Server Endpoints**
   - Health endpoint (`/health`)
   - Info endpoint (`/info`)
   - Admin endpoint (`/healthcheck`) if accessible

3. **Moped Profile Validation**
   - Verify `moped_nl` profile is available
   - Check `moped_access` encoded values are loaded
   - Validate profile configuration

4. **Routing Functionality**
   - Basic routing with generic coordinates
   - Netherlands-specific routing validation
   - **Critical test**: Ensure routes avoid Overijsselselaan

5. **Route Validation**
   - Test coordinates: `53.116614,5.781391` → `53.211454,5.803086`
   - Requirement: Route must NOT use Overijsselselaan
   - Analyzes route instructions to verify street avoidance

## Dependencies

- `docker` - Container management
- `curl` - HTTP requests to GraphHopper API
- `jq` - JSON parsing and validation

## Error Handling

Both scripts include comprehensive error handling:
- Timeout protection for HTTP requests
- Graceful handling of missing data
- Clear error messages and debugging information
- Exit codes that reflect test results

## Integration with CI/CD

The GitHub Action workflow integrates with the existing deployment pipeline:
- Automatically triggered after successful deployment
- Uses the same SSH credentials as deployment
- Provides feedback on deployment quality
- Can prevent broken deployments from going unnoticed

## Monitoring Route Quality

The specific route validation ensures that the moped routing engine properly respects Netherlands road access rules and avoids inappropriate routes like Overijsselselaan for the given coordinates.