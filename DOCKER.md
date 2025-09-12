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

To run the GraphHopper service with Netherlands OSM data using the pre-built image:

```bash
# Download Netherlands OSM data
wget http://download.geofabrik.de/europe/netherlands-latest.osm.pbf

# Copy the sample configuration from the repository
cp -r docker-volume-config graphhopper-data
mv netherlands-latest.osm.pbf graphhopper-data/

# Run the container with GraphHopper volume mounted
docker run -d \
  -p 8989:8989 \
  -v $(pwd)/graphhopper-data:/app/graphhopper \
  ghcr.io/xanox1/graphhopper_mpd:latest
```

### Using Locally Built Image

```bash
# Download Netherlands OSM data
wget http://download.geofabrik.de/europe/netherlands-latest.osm.pbf

# Copy the sample configuration from the repository
cp -r docker-volume-config graphhopper-data
mv netherlands-latest.osm.pbf graphhopper-data/

# Run the container with GraphHopper volume mounted
docker run -d \
  -p 8989:8989 \
  -v $(pwd)/graphhopper-data:/app/graphhopper \
  graphhopper:latest
```

### Using Different OSM Data

If you want to use a different OSM file, you can modify the configuration:

```bash
# Download your preferred OSM data (example with Monaco)
wget http://download.geofabrik.de/europe/monaco-latest.osm.pbf

# Copy the sample configuration from the repository
cp -r docker-volume-config graphhopper-data
mv monaco-latest.osm.pbf graphhopper-data/

# Edit the configuration file to point to your OSM data
sed -i 's/netherlands-latest.osm.pbf/monaco-latest.osm.pbf/g' graphhopper-data/config.yml

# Run the container
docker run -d \
  -p 8989:8989 \
  -v $(pwd)/graphhopper-data:/app/graphhopper \
  ghcr.io/xanox1/graphhopper_mpd:latest
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

- The container expects a configuration file at `/app/graphhopper/config.yml` in the mounted volume
- Copy the sample configuration from `docker-volume-config/` in the repository to get started
- OSM data and graph cache are stored in the `/app/graphhopper` volume
- Custom model files should be placed in `/app/graphhopper/custom_models/` directory
- Graph data cache is stored in `/app/graphhopper/graph-cache` (persisted on the mounted volume)
- Log files are written to `/app/graphhopper/logs/` for persistence
- Set JVM options via the `JAVA_OPTS` environment variable

**Configuration Changes**: With the volume-based configuration, you can modify settings without rebuilding the Docker image. Simply edit the files in your mounted volume and restart the container.

## Memory Requirements

- For small regions (cities): 2-4 GB RAM
- For larger regions (countries): 8-32 GB RAM
- For planet-wide data: 60+ GB RAM

See the [deployment guide](docs/core/deploy.md) for more details.