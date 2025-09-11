# GraphHopper Docker Container

This directory contains a Dockerfile to build a Docker container for the GraphHopper routing engine.

## Building the Docker Image

The GitHub Action in `.github/workflows/docker-build.yml` automatically builds the Docker container on every push to the master branch.

To build manually:

```bash
# Build the project first
mvn clean package -DskipTests

# Build the Docker image
docker build -t graphhopper:latest .
```

## Running the Container

To run the GraphHopper service with your own OpenStreetMap data:

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