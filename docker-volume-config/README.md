# GraphHopper Docker Volume Configuration

**Configuration files are now loaded from the persistent volume** instead of being baked into the Docker image. This allows you to:
- Modify configuration without rebuilding the Docker image  
- Customize routing profiles and models
- Adjust server settings and logging
- Persist configuration changes

## Directory Structure

```
docker-volume-config/
├── config.yml                    # Main GraphHopper configuration file
├── custom_models/                # Directory for custom vehicle models
│   └── moped_nl_model.json       # Complex Netherlands moped routing model
└── README.md                     # This file
```

## Usage

1. **Copy this configuration directory to your desired location**:
   ```bash
   cp -r docker-volume-config my-graphhopper-config
   ```

2. **Add your OSM data file to the configuration directory**:
   ```bash
   wget http://download.geofabrik.de/europe/netherlands-latest.osm.pbf
   mv netherlands-latest.osm.pbf my-graphhopper-config/
   ```

3. **Run the Docker container with the configuration mounted**:
   ```bash
   docker run -d \
     -p 8989:8989 \
     -v $(pwd)/my-graphhopper-config:/app/graphhopper \
     ghcr.io/xanox1/graphhopper_mpd:latest
   ```

## Configuration Files

### config.yml
The main configuration file for GraphHopper. Key settings include:
- **datareader.file**: Path to OSM data file (should be in the mounted volume)
- **graph.location**: Location for graph cache (stored in the volume)
- **custom_models.directory**: Directory containing custom models
- **profiles**: Routing profiles including the moped_nl profile
- **logging**: Log files are written to the volume for persistence

### custom_models/moped_nl_model.json
Example custom model for moped routing with:
- Speed limits appropriate for vehicle regulations
- Access rules for different road types  
- Priority adjustments for vehicle-appropriate routes

## Customization

- **Modify `config.yml`** to adjust GraphHopper settings
- **Add custom model files** to the `custom_models/` directory  
- **Update routing profiles** in the configuration to use your custom models
- **Change log locations** and levels as needed

All changes take effect when the container is restarted - no image rebuild required!