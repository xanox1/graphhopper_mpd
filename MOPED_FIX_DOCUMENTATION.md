# Moped Profile Fix Documentation

## Issue Resolution Summary

The issue `java.lang.IllegalArgumentException: Could not create weighting for profile: 'moped_nl'` with the error message `Cannot compile expression: File 'priority entry', Line 1, Column 6: Not a boolean expression` has been **RESOLVED**.

## Root Cause

The error was caused by an incorrect boolean expression `"!moped_access"` in the custom model configuration. The `moped_access` encoded value is an enum type (`MopedAccess`), not a boolean, so it cannot be used with the `!` (NOT) operator.

The problematic expression was:
```json
{ "if": "!moped_access", "multiply_by": "0" }
```

## Fix Applied

The expression has been corrected to use proper enum comparisons:
```json
{ "if": "moped_access == MISSING || moped_access == NO", "multiply_by": "0" }
```

## Files Updated

1. **`moped_nl_model.json`** - Main moped custom model file
2. **`docker-volume-config/custom_models/moped_nl_model.json`** - Docker volume configuration

## MopedAccess Enum Values

The `moped_access` encoded value supports these enum values:
- `MISSING` - No moped tag present or unrecognized value
- `NO` - moped=no (mopeds not allowed)
- `YES` - moped=yes (mopeds allowed)
- `DESIGNATED` - moped=designated (way designated for mopeds)
- `USE_SIDEPATH` - moped=use_sidepath (mopeds should use sidepath)

## Verification

### Tests
- `MopedProfileValidationTest` - Reproduces the original problem and validates the fix
- `MopedAccessValidationTest` - Tests moped access functionality
- `MopedNlModelIntegrationTest` - Integration test for the complete model

### Server Verification
1. Server starts successfully with moped_nl profile
2. Profile is available in `/info` endpoint
3. All moped_access enum values are properly loaded
4. No compilation errors in logs

### Usage Examples

#### Correct Usage ✅
```json
{
  "priority": [
    { "if": "moped_access == MISSING || moped_access == NO", "multiply_by": "0" },
    { "if": "moped_access == YES || moped_access == DESIGNATED", "multiply_by": "1.2" }
  ]
}
```

#### Incorrect Usage ❌
```json
{
  "priority": [
    { "if": "!moped_access", "multiply_by": "0" }
  ]
}
```

## Deployment Verification

Two verification scripts are provided:

1. **`verify-moped-fix.sh`** - Local verification without Docker
2. **`docker-deployment-verification.sh`** - Docker deployment verification

### Running Verification

```bash
# Local verification
./verify-moped-fix.sh

# Docker verification
./docker-deployment-verification.sh

# Docker verification with custom image
DOCKER_IMAGE=my-custom-image:latest ./docker-deployment-verification.sh
```

## Configuration Requirements

To use the moped_nl profile, ensure your `config.yml` includes:

```yaml
graph.encoded_values: moped_access, car_average_speed, road_access, road_class

profiles:
  - name: moped_nl
    custom_model_files: [moped_nl_model.json]
```

## Alternative: Boolean Moped Access

For simple boolean checks, use `moped_vehicle_access` instead:

```yaml
graph.encoded_values: moped_vehicle_access, car_average_speed
```

```json
{
  "priority": [
    { "if": "!moped_vehicle_access", "multiply_by": "0" }
  ]
}
```

Note: `moped_vehicle_access` is a boolean encoded value and supports the `!` operator, unlike the enum-based `moped_access`.

## References

- [Moped Access Usage Documentation](docs/moped-access-usage.md)
- [Custom Models Documentation](docs/core/custom-models.md)
- [GraphHopper Configuration](config-docker.yml)