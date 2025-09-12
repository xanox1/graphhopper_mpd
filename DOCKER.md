# GraphHopper Docker Container

This directory contains a Dockerfile to build a Docker container for the GraphHopper routing engine.

## Building the Docker Image

The GitHub Action in `.github/workflows/docker-build.yml` automatically builds and pushes the Docker container to GitHub Container Registry (GHCR). The workflow runs:

- **On pull requests**: Builds and tests Docker images (validation only, no push)
- **On direct pushes to master**: Builds and pushes images to GHCR  
- **When PRs are merged to master**: Builds and pushes images to GHCR

Images are available at `ghcr.io/xanox1/graphhopper_mpd`.

**Note**: Images in GHCR are private by default and require authentication to pull. The deployment workflow in `.github/workflows/deploy-to-docker-host.yml` handles this automatically using GitHub tokens.

### Building Manually

To build manually:

```bash
# Build the project first
mvn clean package -DskipTests

# Build the Docker image
docker build -t graphhopper:latest .
```

**Note**: The Dockerfile uses a Docker-specific configuration (`config-docker.yml`) that binds the server to `0.0.0.0` instead of `localhost` to allow external connections to the container.

## Running the Container

### Using Pre-built Image from GHCR

To run the GraphHopper service with your own OpenStreetMap data using the pre-built image:

```bash
# Download OSM data (example with Monaco)
wget http://download.geofabrik.de/europe/monaco-latest.osm.pbf

# Run the container with OSM data mounted
docker run -d \
  -p 8989:8989 \
  -v $(pwd)/monaco-latest.osm.pbf:/app/data.osm.pbf \
  -e JAVA_OPTS="-Xmx2g -Xms2g -Ddw.graphhopper.datareader.file=/app/data.osm.pbf" \
  ghcr.io/xanox1/graphhopper_mpd:latest
```

### Using Locally Built Image

```bash
# Download OSM data (example with Monaco)
wget http://download.geofabrik.de/europe/monaco-latest.osm.pbf

# Run the container with OSM data mounted
docker run -d \
  -p 8989:8989 \
  -v $(pwd)/monaco-latest.osm.pbf:/app/data.osm.pbf \
  -e JAVA_OPTS="-Xmx2g -Xms2g -Ddw.graphhopper.datareader.file=/app/data.osm.pbf" \
  graphhopper:latest
```

The service will be available at http://localhost:8989

## Verification

To verify your Docker deployment is working correctly, use the provided verification script:

```bash
# Basic verification
./verify-deployment.sh

# Verify specific container
./verify-deployment.sh my-graphhopper-container

# Verify remote deployment
./verify-deployment.sh graphhopper your-server.com 8989
```

The script will check:
- Container is running
- Health endpoints are responding
- API endpoints are accessible  
- Container logs for errors

## Configuration

- The container uses `/app/config.yml` as the default configuration
- Mount your own config file to override: `-v /path/to/your/config.yml:/app/config.yml`
- Graph data is stored in `/app/graph-cache`
- Set JVM options via the `JAVA_OPTS` environment variable

## Memory Requirements

- For small regions (cities): 2-4 GB RAM
- For larger regions (countries): 8-32 GB RAM
- For planet-wide data: 60+ GB RAM

See the [deployment guide](docs/core/deploy.md) for more details.