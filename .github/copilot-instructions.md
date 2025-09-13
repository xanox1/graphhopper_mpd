# GraphHopper Moped Routing Engine (Netherlands Focus)

**PRIMARY FOCUS**: This is a specialized GraphHopper fork optimized for moped routing in the Netherlands with complex routing strategies. The application includes advanced moped access parsing, Netherlands-specific speed limits, cycleway access rules, and custom routing models that comply with Dutch moped regulations.

GraphHopper MPD extends the standard GraphHopper routing engine with sophisticated moped-specific features including custom access parsers, specialized OSM tag interpretation for moped permissions, and complex routing algorithms that handle the unique legal requirements for moped travel in the Netherlands (including cycleway access where permitted).

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Moped Routing Specialization

### Netherlands Moped Regulations Integration
This repository implements complex Netherlands moped routing with:
- **Legal cycleway access**: Routes mopeds on cycleways where `moped=yes` or `moped=designated` in OSM
- **Speed limit compliance**: 45 km/h general limit, 30 km/h residential, 25 km/h cycleways, 15 km/h living streets
- **Access rule parsing**: Sophisticated OSM tag interpretation for moped permissions
- **Priority routing**: Higher priority for designated moped routes and appropriate road classes

### Moped-Specific Configuration Examples
```yaml
# Required in config.yml for moped routing
graph.encoded_values: car_access, car_average_speed, road_access, moped_access, road_class

profiles:
  - name: moped_nl
    custom_model_files: [moped_nl_model.json]
```

### Complex Routing Model (`moped_nl_model.json`)
- **Distance influence**: 90% (prefers shorter routes)
- **Access filtering**: Blocks routes where `!moped_access`
- **Priority boosts**: 1.2x for `moped_access == YES || moped_access == DESIGNATED`
- **Cycleway handling**: Special rules for `road_class == CYCLEWAY && moped_access == YES`
- **Speed restrictions**: Multiple conditional speed limits based on road class

### Validation Commands for Moped Features
```bash
# Test moped routing between Dutch cities
curl "http://localhost:8989/route?point=52.370216,4.895168&point=52.520008,6.083887&profile=moped_nl"

# Compare with car routing to verify different logic
curl "http://localhost:8989/route?point=52.370216,4.895168&point=52.520008,6.083887&profile=car"

# Verify moped_access encoded values are loaded
curl "http://localhost:8989/info" | grep moped_access
```

## Working Effectively

### Prerequisites and Setup
- Install Java 17 or higher (required). Current development uses OpenJDK 17.0.16.
- Maven 3.9+ is required for building.
- No additional tools are required for basic development.

### Build Commands (NEVER CANCEL - Use Long Timeouts)
- `mvn clean compile -DskipTests` - Compile only: takes 1 minute 25 seconds. NEVER CANCEL. Set timeout to 5+ minutes.
- `mvn clean test` - Full test suite: takes 3 minutes 45 seconds. NEVER CANCEL. Set timeout to 10+ minutes. 
- `mvn clean test verify` - Complete verification: takes 4 minutes 30 seconds. NEVER CANCEL. Set timeout to 10+ minutes.
- `mvn clean install -DskipTests` - Build and package: takes 1 minute 35 seconds. NEVER CANCEL. Set timeout to 5+ minutes.

### Running the Application (Moped-Focused)
1. Build the application first: `mvn clean install -DskipTests`
2. **For Netherlands moped routing**: Use Netherlands OSM data for production: `wget http://download.geofabrik.de/europe/netherlands-latest.osm.pbf`
3. **For development/testing**: Use included test data: `core/files/andorra.osm.pbf` (small, fast) for basic functionality testing
4. **Start with moped profile**: `java -Ddw.graphhopper.datareader.file=netherlands-latest.osm.pbf -jar web/target/graphhopper-web-11.0-SNAPSHOT.jar server config-docker.yml`
5. **Alternative for testing**: `java -Ddw.graphhopper.datareader.file=core/files/andorra.osm.pbf -jar web/target/graphhopper-web-11.0-SNAPSHOT.jar server config-example.yml`
6. Server starts on port 8989, admin on port 8990  
7. Wait for "Started Server" message before testing endpoints
8. **Moped profile preparation**: First run imports OSM data and builds moped-specific routing graph (can take significant time for Netherlands data)

### Docker Support (Netherlands Production)
- Repository includes Docker configuration optimized for Netherlands moped routing
- Production setup uses `config-docker.yml` with Netherlands OSM data (`netherlands-latest.osm.pbf`)
- Volume-based configuration in `docker-volume-config/` allows runtime customization
- Use `verify-deployment.sh` script to validate Docker deployments
- Images available at `ghcr.io/xanox1/graphhopper_mpd`
- Container includes moped_nl profile and complex routing models by default

## Validation

### Essential Endpoints to Test After Changes (Moped Focus)
Always test these endpoints after making changes:
- Health check: `curl http://localhost:8989/health` (should return "OK")
- Server info: `curl http://localhost:8989/info` (returns version, profiles including moped_nl, encoded values)
- **Moped routing API**: `curl "http://localhost:8989/route?point=52.370216,4.895168&point=52.520008,6.083887&profile=moped_nl"` (Amsterdam to Groningen)
- **Car routing for comparison**: `curl "http://localhost:8989/route?point=52.370216,4.895168&point=52.520008,6.083887&profile=car"`
- Web UI: Access `http://localhost:8989/maps/` in browser (should load GraphHopper Maps interface with moped_nl profile available)

### Manual Validation Scenarios (Moped-Specific)
ALWAYS manually validate functionality after making changes:
1. **Server Startup**: Start server with Netherlands data and verify no errors in logs, ensure moped_access encoded values are loaded
2. **Moped Routing Functionality**: Test moped routing between Dutch cities using moped_nl profile
3. **Complex Routing Validation**: Verify moped routes properly handle cycleway access, speed restrictions, and access permissions
4. **Profile Support**: Verify moped_nl profile works correctly, compare routes with car profile to ensure different routing logic
5. **Access Rules**: Test that moped routing correctly interprets OSM moped access tags (moped=yes, moped=designated, moped=no)
6. **Web Interface**: Load maps UI and verify moped_nl profile is available in dropdown, test routing with moped profile

### Code Quality
- Run `mvn clean test verify` before committing - ensures all tests pass. NEVER CANCEL builds.
- Note: `mvn checkstyle:check` currently fails due to Java record syntax compatibility - this is a known issue and does not affect functionality
- Follow Java coding standards: 4 space indentation, 100 character line width
- Use IntelliJ defaults for formatting

## Common Tasks

### Repository Structure
```
/home/runner/work/graphhopper_mpd/graphhopper_mpd/
├── core/                    # Core routing algorithms and data structures
├── web-api/                # Web API definitions and data models  
├── web-bundle/             # Dropwizard application bundle
├── web/                    # Web service implementation
├── reader-gtfs/            # GTFS (public transit) data reader
├── tools/                  # Command line tools and utilities
├── map-matching/           # GPS trace matching to roads
├── client-hc/              # HTTP client for API
├── navigation/             # Turn-by-turn navigation
├── example/                # Example usage code
├── config-example.yml      # Example configuration file
└── verify-deployment.sh    # Docker deployment verification
```

### Key Configuration Files (Moped-Focused)
- `config-docker.yml` - Production configuration with moped_nl profile and Netherlands OSM data
- `config-example.yml` - Development configuration (generic profiles)
- `moped_nl_model.json` - **Core moped routing model** with Netherlands-specific speed limits and access rules
- `docker-volume-config/` - Docker volume configuration with moped-optimized settings
- `pom.xml` - Maven parent project configuration  
- `.github/workflows/build.yml` - CI/CD pipeline configuration

### Test Data Available in Repository (Moped Development)
- **Netherlands production data**: Download `netherlands-latest.osm.pbf` from Geofabrik for full moped routing testing
- `core/files/andorra.osm.pbf` - Small test dataset (recommended for development, basic functionality testing)
- `core/files/one_way_dead_end.osm.pbf` - Edge case testing
- `map-matching/files/leipzig_germany.osm.pbf` - Map matching tests
- `reader-gtfs/files/beatty.osm` - GTFS testing
- `moped_nl_model.json` - **Production moped routing model** for Netherlands with complex access rules

### Generated Artifacts
After building with `mvn clean install`:
- `web/target/graphhopper-web-11.0-SNAPSHOT.jar` - Main web service
- `web-bundle/target/graphhopper-web-bundle-11.0-SNAPSHOT.jar` - Bundle version
- All module JARs in respective target/ directories

### Module Purposes (Moped Extensions)
- **core**: Core routing engine, algorithms, data structures + **moped-specific access parsers and routing logic**
- **web-api**: REST API models and contracts
- **web-bundle**: Dropwizard application configuration  
- **web**: HTTP service implementation and resources
- **reader-gtfs**: Public transit routing support
- **tools**: Command line utilities and benchmarking
- **map-matching**: GPS trace to road matching
- **client-hc**: Java HTTP client library
- **navigation**: Turn-by-turn instructions
- **example**: Sample code and documentation

#### Moped-Specific Components
- `MopedAccess.java` - Enum for moped access permissions (MISSING, NO, YES, DESIGNATED, USE_SIDEPATH)
- `MopedAccessParser.java` - Parser for OSM moped access tags
- `MopedVehicleAccessParser.java` - Vehicle-specific moped access parsing
- `moped_nl_model.json` - Complex Netherlands moped routing model with speed limits and access rules

### Development Workflow (Moped-Focused)
1. Make code changes in appropriate module (especially moped-specific components in core/)
2. Run `mvn clean test verify` to validate changes (takes ~4.5 minutes, NEVER CANCEL)
3. **Test moped functionality**: Start server with Netherlands data and exercise moped_nl profile endpoints
4. **Validate complex routing**: Test moped routing scenarios including cycleway access and speed restrictions
5. For web changes, test both API and UI functionality with moped profile
6. Check logs for any warnings or errors during server startup, especially moped access parsing

### Common Issues and Solutions (Moped-Specific)
- **Server won't start**: Check Java version (must be 17+), verify OSM data file exists, ensure moped_access encoded values are properly configured
- **Moped routing fails**: Verify moped_access is included in graph.encoded_values configuration, check moped_nl_model.json is accessible
- **Tests fail**: Ensure clean build with `mvn clean test verify`, check moped-specific test components
- **Complex routing issues**: Verify OSM data includes proper moped access tags, check custom model logic for Netherlands rules
- **Checkstyle errors**: Known issue with Java records, safe to ignore for functionality
- **Out of memory**: Increase JVM heap size with `-Xmx` parameter (Netherlands OSM data requires significant memory)
- **Port conflicts**: Default ports 8989/8990, check for conflicts
- **Missing moped profile**: Ensure config includes moped_nl profile and custom_models.directory points to correct location

### Performance Notes (Moped Routing Optimizations)
- GraphHopper uses RAM_STORE by default for fast access
- Contraction Hierarchies (CH) preparation speeds up routing but uses more memory
- **Moped routing complexity**: Custom models with complex access rules may impact performance
- Hybrid mode (Landmarks) provides balance between speed and flexibility for moped profiles
- **Netherlands OSM data**: Large file requires significant RAM during import phase (4GB+ recommended)
- **Moped access parsing**: Additional encoded values increase memory usage but enable sophisticated routing

### IDE Setup
- IntelliJ IDEA and NetBeans work out of the box
- Eclipse requires additional setup (see docs/core/eclipse-setup.md)
- Use project's .editorconfig for consistent formatting