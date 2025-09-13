# GraphHopper Routing Engine

GraphHopper is a fast and memory-efficient routing engine written in Java that provides routing, map matching, and isochrone services. It uses OpenStreetMap data and supports multiple vehicle profiles including car, bike, foot, and truck routing.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

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

### Running the Application
1. Build the application first: `mvn clean install -DskipTests`
2. Use test data from the repository: `core/files/andorra.osm.pbf` (small, fast) or download larger datasets
3. Start the server: `java -Ddw.graphhopper.datareader.file=core/files/andorra.osm.pbf -jar web/target/graphhopper-web-11.0-SNAPSHOT.jar server config-example.yml`
4. Server starts on port 8989, admin on port 8990
5. Wait for "Started Server" message before testing endpoints

### Docker Support
- Repository includes Docker configuration with volume-based setup
- Use `verify-deployment.sh` script to validate Docker deployments
- Images available at `ghcr.io/xanox1/graphhopper_mpd`

## Validation

### Essential Endpoints to Test After Changes
Always test these endpoints after making changes:
- Health check: `curl http://localhost:8989/health` (should return "OK")
- Server info: `curl http://localhost:8989/info` (returns version, profiles, encoded values)
- Routing API: `curl "http://localhost:8989/route?point=42.508552,1.518921&point=42.530262,1.545162&profile=car"`
- Web UI: Access `http://localhost:8989/maps/` in browser (should load GraphHopper Maps interface)

### Manual Validation Scenarios
ALWAYS manually validate functionality after making changes:
1. **Server Startup**: Start server with test data and verify no errors in logs
2. **Routing Functionality**: Test routing between two coordinates using the API
3. **Profile Support**: Verify car profile works (default), test other profiles if modified
4. **Web Interface**: Load maps UI and verify it displays correctly

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

### Key Configuration Files
- `config-example.yml` - Main configuration with server settings, profiles, and data sources
- `pom.xml` - Maven parent project configuration
- `.github/workflows/build.yml` - CI/CD pipeline configuration

### Test Data Available in Repository
- `core/files/andorra.osm.pbf` - Small test dataset (recommended for development)
- `core/files/one_way_dead_end.osm.pbf` - Edge case testing
- `map-matching/files/leipzig_germany.osm.pbf` - Map matching tests
- `reader-gtfs/files/beatty.osm` - GTFS testing

### Generated Artifacts
After building with `mvn clean install`:
- `web/target/graphhopper-web-11.0-SNAPSHOT.jar` - Main web service
- `web-bundle/target/graphhopper-web-bundle-11.0-SNAPSHOT.jar` - Bundle version
- All module JARs in respective target/ directories

### Module Purposes
- **core**: Core routing engine, algorithms, data structures
- **web-api**: REST API models and contracts
- **web-bundle**: Dropwizard application configuration  
- **web**: HTTP service implementation and resources
- **reader-gtfs**: Public transit routing support
- **tools**: Command line utilities and benchmarking
- **map-matching**: GPS trace to road matching
- **client-hc**: Java HTTP client library
- **navigation**: Turn-by-turn instructions
- **example**: Sample code and documentation

### Development Workflow
1. Make code changes in appropriate module
2. Run `mvn clean test verify` to validate changes (takes ~4.5 minutes, NEVER CANCEL)
3. Test functionality by starting server and exercising API endpoints
4. For web changes, test both API and UI functionality
5. Check logs for any warnings or errors during server startup

### Common Issues and Solutions
- **Server won't start**: Check Java version (must be 17+), verify OSM data file exists
- **Tests fail**: Ensure clean build with `mvn clean test verify`
- **Checkstyle errors**: Known issue with Java records, safe to ignore for functionality
- **Out of memory**: Increase JVM heap size with `-Xmx` parameter
- **Port conflicts**: Default ports 8989/8990, check for conflicts

### Performance Notes
- GraphHopper uses RAM_STORE by default for fast access
- Contraction Hierarchies (CH) preparation speeds up routing but uses more memory
- Hybrid mode (Landmarks) provides balance between speed and flexibility
- Large OSM files require significant RAM during import phase

### IDE Setup
- IntelliJ IDEA and NetBeans work out of the box
- Eclipse requires additional setup (see docs/core/eclipse-setup.md)
- Use project's .editorconfig for consistent formatting