# Moped Access Encoded Value Usage

This document explains the proper usage of the `moped_access` encoded value in custom models.

## Overview

The `moped_access` encoded value is an enum-type that represents moped access permissions on ways based on the OSM "moped" tag. It cannot be used with boolean operators like `!` - instead, it must be compared to specific enum values.

## Enum Values

The MopedAccess enum has the following values:
- `MISSING` - No moped tag present or unrecognized value
- `NO` - moped=no (mopeds not allowed)
- `YES` - moped=yes (mopeds allowed)
- `DESIGNATED` - moped=designated (way designated for mopeds)
- `USE_SIDEPATH` - moped=use_sidepath (mopeds should use sidepath)

## Correct Usage

### ✅ CORRECT: Enum comparisons
```json
{
  "priority": [
    { "if": "moped_access == MISSING || moped_access == NO", "multiply_by": "0" },
    { "if": "moped_access == YES || moped_access == DESIGNATED", "multiply_by": "1.2" }
  ]
}
```

### ❌ INCORRECT: Boolean operations
```json
{
  "priority": [
    { "if": "!moped_access", "multiply_by": "0" }
  ]
}
```

## Common Patterns

### Block access when moped is not allowed
```json
{ "if": "moped_access == MISSING || moped_access == NO", "multiply_by": "0" }
```

### Prefer designated moped ways
```json
{ "if": "moped_access == DESIGNATED", "multiply_by": "1.5" }
```

### Allow but discourage use_sidepath
```json
{ "if": "moped_access == USE_SIDEPATH", "multiply_by": "0.5" }
```

## Configuration

To use moped_access in your custom model, ensure your config.yml includes:

```yaml
graph.encoded_values: moped_access, car_average_speed, road_access, road_class
```

## Alternative: Boolean Moped Access

If you need a simple boolean moped access check, use `moped_vehicle_access` instead:

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

Note: `moped_vehicle_access` is a boolean encoded value and supports `!` operator.